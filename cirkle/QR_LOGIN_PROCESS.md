# QR Login Process & User Search Flow

## 1. High-Level Overview

The QR login process is a direct authentication flow that allows volunteers to log in by scanning a QR code containing their `volunteerId` and `eventId`. Unlike phone OTP login which requires OTP verification, QR login provides immediate access if the volunteer is registered and active.

---

## 2. QR Login Request Flow (Start to Finish)

### Frontend (Mobile App):

```
User scans QR code
    ↓
Extract volunteerId and eventId from QR data (format: "volunteerId::eventId")
    ↓
Validate event exists (POST /volunteer/validate-event/{eventId})
    ↓
Call QR login endpoint (POST /volunteer/login-qr)
    ↓
Receive authentication tokens (access token + refresh token)
    ↓
Store tokens in AsyncStorage
    ↓
Navigate to dashboard
```

### Backend Endpoint:

```
POST /volunteer/login-qr
Content-Type: application/json

{
  "volunteerId": "vol_123abc",
  "eventId": "event_456def"
}
```

---

## 3. Backend QR Login Process (Step-by-Step)

### Step 1: Request Validation

**File**: `VolunteerController.java`

```java
@PostMapping("/login-qr")
@Operation(summary = "QR Code Login")
public ResponseEntity<?> loginWithQr(@RequestBody QrLoginRequest request) {
    // Calls service layer
    VolunteerResponse response = volunteerService.loginWithQr(request);

    // Returns with appropriate HTTP status
    if (response.getSuccess()) {
        return ResponseEntity.ok(response);  // 200 OK with tokens
    } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);  // 401 Unauthorized
    }
}
```

**Request DTO**:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrLoginRequest {
    private String volunteerId;  // Unique volunteer identifier
    private String eventId;      // Event this volunteer is registered for
}
```

---

### Step 2: Event Validation

**File**: `VolunteerService.java` - `loginWithQr()` method

```java
// Check if event exists in system
if (eventRepository.findById(request.getEventId()).isEmpty()) {
    return VolunteerResponse.builder()
            .success(false)
            .message("Event not found")
            .build();
}
```

**Database Query**:

- Uses `eventRepository.findById(eventId)`
- Checks `Event` table for a record with matching `eventId`
- If not found → Return 401 error "Event not found"
- **Purpose**: Ensure the event is legitimate and active

---

### Step 3: Volunteer Validation & Search (PRIMARY USER SEARCH)

**File**: `VolunteerService.java`

```java
// MAIN SEARCH: Find volunteer by volunteerId
Optional<Volunteer> volunteerOpt = volunteerRepository.findById(request.getVolunteerId());

if (volunteerOpt.isEmpty()) {
    return VolunteerResponse.builder()
            .success(false)
            .message("Volunteer not found")
            .build();
}

Volunteer volunteer = volunteerOpt.get();

// Check if volunteer is active for this event
if (!volunteer.getIsActive()) {
    return VolunteerResponse.builder()
            .success(false)
            .message("Volunteer is not active for this event")
            .build();
}
```

**Database Search Details**:

| Search Operation   | Database Table | Search Method   | Query                                             |
| ------------------ | -------------- | --------------- | ------------------------------------------------- |
| **Primary Search** | `Volunteer`    | `findById()`    | `SELECT * FROM volunteers WHERE volunteer_id = ?` |
| **Validation**     | `Volunteer`    | `getIsActive()` | Check boolean field `is_active = true`            |

**Volunteer Entity Fields**:

```java
@Entity
class Volunteer {
    @Id
    private String volunteerId;        // Primary key - UNIQUE identifier
    private String userId;             // Link to User (foreign key)
    private String eventId;            // Event association
    private String phone;              // Phone number
    private String name;               // Volunteer name
    private String email;              // Email address
    private String role;               // Role (e.g., "VOLUNTEER")
    private Boolean isActive;          // Activity status for this event
}
```

**What this tells us about the volunteer**:

- ✅ Volunteer exists in system
- ✅ Volunteer is registered for this specific event
- ✅ Volunteer is active/not deactivated
- ✅ We get their `userId` to fetch/create User record

---

### Step 4: User Lookup or Creation (SECONDARY USER SEARCH)

**File**: `VolunteerService.java`

```java
// SECONDARY SEARCH: Find User by userId from volunteer record
Optional<User> userOpt = userRepository.findById(volunteer.getUserId());

User user;
if (userOpt.isEmpty()) {
    // User doesn't exist - CREATE NEW USER
    String phoneDigits = volunteer.getPhone().replaceAll("\\D", "");

    user = User.builder()
            .userId(volunteer.getUserId())          // Use same ID as volunteer
            .name(volunteer.getName())              // From volunteer record
            .email(volunteer.getEmail())            // From volunteer record
            .phone(volunteer.getPhone())            // From volunteer record
            .username("qr_user_" + phoneDigits)    // Auto-generated username
            .role(UserRole.USER)                    // Standard user role
            .authMethod(AuthMethod.QR)              // Mark as QR authenticated
            .isVerified(true)                       // Auto-verified via QR
            .isActive(true)
            .build();

    user = userRepository.save(user);  // Write to database
    log.info("Created new user for QR login: {}", user.getUserId());
} else {
    // User exists - use existing record
    user = userOpt.get();
}
```

**Database Search Details**:

| Search Operation | Database Table | Search Method | Query                                   |
| ---------------- | -------------- | ------------- | --------------------------------------- |
| **User Lookup**  | `Users`        | `findById()`  | `SELECT * FROM users WHERE user_id = ?` |

**User Entity Fields**:

```java
@Entity
class User {
    @Id
    private String userId;         // Primary key
    private String name;           // Full name
    private String email;          // Email address
    private String phone;          // Phone number
    private String username;       // Username
    private UserRole role;         // USER, ADMIN, STAFF
    private AuthMethod authMethod; // QR, PHONE, EMAIL, etc.
    private Boolean isVerified;    // Email/phone verified
    private Boolean isActive;      // Account active status
}
```

**Why create a user if missing?**

- QR login shortcut the standard signup process
- User record is needed for JWT token generation
- Authentication system requires a User entity
- Phone and email already available from Volunteer record

---

### Step 5: Token Generation & Response

**File**: `VolunteerService.java`

```java
// Generate JWT tokens
String token = jwtTokenProvider.generateToken(
    user.getUserId(),      // Subject (who this token is for)
    user.getEmail(),       // Email claim
    user.getPhone()        // Phone claim
);

String refreshToken = jwtTokenProvider.generateRefreshToken(
    user.getUserId()
);

// Build response with all volunteer data + tokens
return VolunteerResponse.builder()
        .success(true)
        .volunteerId(volunteer.getVolunteerId())    // From volunteer record
        .userId(user.getUserId())                   // From user record
        .eventId(volunteer.getEventId())            // Event association
        .phone(volunteer.getPhone())                // Contact info
        .name(volunteer.getName())                  // Display name
        .email(volunteer.getEmail())                // Contact email
        .role(volunteer.getRole())                  // Volunteer role
        .token(token)                               // ✅ ACCESS TOKEN (15 min)
        .refreshToken(refreshToken)                 // ✅ REFRESH TOKEN (7 days)
        .isActive(volunteer.getIsActive())          // Status
        .message("QR login successful")
        .build();
```

**Token Details**:

- **Access Token**: 15-minute expiration, used to make API calls
- **Refresh Token**: 7-day expiration, used to get new access tokens
- **Token Payload**:
  ```json
  {
    "sub": "user_123", // userId
    "exp": 1234567890, // Expiration timestamp
    "email": "user@example.com",
    "phone": "+919876543210"
  }
  ```

---

## 4. User Search Strategy Comparison

### Phone OTP Login (For Reference)

```
Phone number provided → Search User by phone → Verify/Create → Generate tokens
```

### QR Login (Current Implementation)

```
QR Code scanned
    ↓
Extract volunteerId & eventId
    ↓
[PRIMARY SEARCH] volunteerRepository.findById(volunteerId)
    {
        Purpose: Verify volunteer exists & is active
        Returns: Volunteer record with userId
    }
    ↓
[SECONDARY SEARCH] userRepository.findById(volunteer.getUserId())
    {
        Purpose: Fetch or create User for JWT generation
        If missing: Auto-create User from Volunteer data
    }
    ↓
Generate tokens using User record
    ↓
Return complete VolunteerResponse with tokens
```

---

## 5. Error Scenarios & Responses

### Scenario 1: Event Not Found

```json
Status: 401 Unauthorized
{
  "success": false,
  "message": "Event not found"
}
```

### Scenario 2: Volunteer Not Found

```json
Status: 401 Unauthorized
{
  "success": false,
  "message": "Volunteer not found"
}
```

### Scenario 3: Volunteer Inactive

```json
Status: 401 Unauthorized
{
  "success": false,
  "message": "Volunteer is not active for this event"
}
```

### Scenario 4: Unexpected Error

```json
Status: 500 Internal Server Error
{
  "success": false,
  "message": "Error during QR login"
}
```

### Scenario 5: Success

```json
Status: 200 OK
{
  "success": true,
  "volunteerId": "vol_123abc",
  "userId": "user_456def",
  "eventId": "event_789ghi",
  "phone": "+919876543210",
  "name": "John Volunteer",
  "email": "john@example.com",
  "role": "VOLUNTEER",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "isActive": true,
  "message": "QR login successful"
}
```

---

## 6. Key Differences from Phone OTP Login

| Aspect                   | QR Login                           | Phone OTP Login                  |
| ------------------------ | ---------------------------------- | -------------------------------- |
| **Verification**         | No OTP verification needed         | Requires OTP verification first  |
| **Entry Point**          | Direct access via QR code          | Phone number + OTP               |
| **Volunteer Lookup**     | By `volunteerId` from QR           | By phone number                  |
| **User Creation**        | Auto-create if missing             | Create or use existing           |
| **Authentication Speed** | Instant (1 API call)               | Two calls (send OTP, verify OTP) |
| **Security Level**       | QR is unique to volunteer          | Phone could be shared            |
| **Primary Use**          | Event check-in, staff registration | General volunteer signup         |

---

## 7. Database Relationships

```
┌─────────────┐         ┌──────────────┐         ┌────────┐
│  Volunteer  │◄───────┤     User      │         │ Event  │
├─────────────┤        └──────────────┘         └────────┘
│ volunterId* │                                      △
│ userId (FK) │                                      │
│ eventId     │                                      │
│ name        │                                      │
│ email       │                                      │
│ phone       │                                      │
│ isActive    │                                      │
└─────────────┘                                      │
      │                                              │
      └──────────────────────────────────────────────┘

* volunteerId is PRIMARY KEY
* userId is FOREIGN KEY pointing to User.userId
* eventId is FOREIGN KEY pointing to Event.eventId
```

---

## 8. Logging & Debugging

The implementation includes logging at critical points:

```java
// User creation logged
log.info("Created new user for QR login: {}", user.getUserId());

// Errors logged
log.error("Error during QR login: {}", e.getMessage());
```

**Frontend Logs** (in React Native app):

```
📱 QR CODE SCANNED FOR LOGIN
QR Data: vol_123abc::event_456def

📋 QR Login Response Received
Volunteer ID: vol_123abc
Name: John Volunteer
Phone: +919876543210
Has Token: true
Has RefreshToken: true
Token length: 256
RefreshToken length: 320

✅ QR Login Successful
Volunteer ID: vol_123abc
Event ID: event_456def
```

---

## 9. Summary

### User Search Process in QR Login:

1. **Primary Search (Volunteer)**: Find volunteer by `volunteerId` from QR code
   - Query: `volunteerRepository.findById(volunteerId)`
   - Validates: Exists + Active + Associated with event

2. **Secondary Search (User)**: Find user by `userId` from volunteer record
   - Query: `userRepository.findById(volunteer.getUserId())`
   - If missing: Auto-create from volunteer data
   - Purpose: Get/create User entity for JWT token generation

3. **Result**: Return volunteer + authentication tokens for immediate access

This two-layer search ensures both the volunteer is properly registered AND the user account exists for JWT token generation.
