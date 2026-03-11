# IMPLEMENTATION SUMMARY: Wallet-Based QR & Phone Login

## Date: February 22, 2026

---

## CHANGES MADE

### 1. Backend DTOs & Models

#### Created: `QrLoginRequest.java` (UPDATED)

```java
public class QrLoginRequest {
    private String qrString;  // Raw QR string from wallet
}
```

**Changed from**: Accepted `volunteerId` and `eventId`
**Now**: Only accepts `qrString` for wallet-based search

#### Created: `PhoneLoginRequest.java` (NEW)

```java
public class PhoneLoginRequest {
    private String phone;  // Phone with +91 prefix
}
```

**Purpose**: Support phone login for TopUp users

#### Updated: `VolunteerResponse.java`

**Added fields**:

- `String walletId` - ID of the wallet for this user
- `Double balance` - Current wallet balance
- `String userName` - User's display name

---

### 2. Backend Service Layer

#### Updated: `VolunteerService.java`

**Added imports**:

- `import com.razespace.cirklebackend.wallet.entity.Wallet;`
- `import com.razespace.cirklebackend.wallet.repository.WalletRepository;`

**Added field**:

```java
private final WalletRepository walletRepository;
```

**Method: `loginWithQr()` (COMPLETELY REWRITTEN)**

**Old Flow (Removed)**:

```
volunteerId + eventId → find volunteer → create/get user → tokens
```

**New Flow (Wallet-Based)**:

```
qrString → search wallet by qrString → get userId from wallet →
validate user exists → generate tokens → return user + wallet info
```

**Implementation**:

```java
public VolunteerResponse loginWithQr(QrLoginRequest request) {
    // STEP 1: Search wallet by qrString
    Optional<Wallet> walletOpt = walletRepository.findByQrString(request.getQrString());
    if (walletOpt.isEmpty()) {
        return error("QR not valid - Wallet not found");
    }

    Wallet wallet = walletOpt.get();
    String userIdFromWallet = wallet.getUserId();

    // STEP 2: Validate user exists
    Optional<User> userOpt = userRepository.findById(userIdFromWallet);
    if (userOpt.isEmpty()) {
        return error("User not found - QR not valid");
    }

    User user = userOpt.get();

    // STEP 3: Generate tokens
    String token = jwtTokenProvider.generateToken(...);
    String refreshToken = jwtTokenProvider.generateRefreshToken(...);

    // STEP 4: Return response with wallet info
    return VolunteerResponse.builder()
        .success(true)
        .userId(user.getUserId())
        .walletId(wallet.getId())
        .balance(wallet.getBalance())
        .token(token)
        .refreshToken(refreshToken)
        .message("QR login successful")
        .build();
}
```

**Method: `loginWithPhone()` (NEW)**

**Purpose**: Allow TopUp users to login with phone and see existing wallet (if any)

**Flow**:

```
phone → search user by phone → find wallet (optional) →
generate tokens → return user + wallet info (if exists)
```

**Implementation**:

```java
public VolunteerResponse loginWithPhone(PhoneLoginRequest request) {
    // STEP 1: Search user by phone
    Optional<User> userOpt = userRepository.findByPhone(request.getPhone());
    if (userOpt.isEmpty()) {
        return error("User not found with this phone");
    }

    User user = userOpt.get();

    // STEP 2: Find wallet (optional - may not exist yet)
    List<Wallet> wallets = walletRepository.findByUserId(user.getUserId());

    String walletId = null;
    Double balance = null;

    if (!wallets.isEmpty()) {
        Wallet wallet = wallets.get(0);
        walletId = wallet.getId();
        balance = wallet.getBalance();
    }

    // STEP 3: Generate tokens
    String token = jwtTokenProvider.generateToken(...);
    String refreshToken = jwtTokenProvider.generateRefreshToken(...);

    // STEP 4: Return response
    // walletId and balance will be null if no wallet exists
    return VolunteerResponse.builder()
        .success(true)
        .userId(user.getUserId())
        .walletId(walletId)
        .balance(balance)
        .token(token)
        .refreshToken(refreshToken)
        .message(walletId != null ? "Login successful" : "First topup - wallet will be created")
        .build();
}
```

---

### 3. Backend Controller

#### Updated: `VolunteerController.java`

**Endpoint: `POST /api/volunteer/login-qr` (UPDATED)**

```
Request: { "qrString": "..." }
Response: {
  "success": true,
  "userId": "user_123",
  "walletId": "wallet_456",
  "balance": 1500.00,
  "userName": "John User",
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "message": "QR login successful"
}
```

**Endpoint: `POST /api/volunteer/login-phone` (NEW)**

```
Request: { "phone": "+919876543210" }
Response: {
  "success": true,
  "userId": "user_123",
  "walletId": "wallet_456" or null,
  "balance": 500.00 or null,
  "userName": "John User",
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "message": "Login successful" or "First topup - wallet will be created"
}
```

---

### 4. Frontend Changes

#### Updated: `volunteer-request.ts`

**Method: `loginWithQr()` (SIGNATURE CHANGED)**

```typescript
// OLD: loginWithQr(volunteerId: string, eventId: string)
// NEW:
loginWithQr: async (qrString: string) => {
  const response = await api.post('/login-qr', { qrString });
  return response.data;
};
```

#### Updated: `login-qr.tsx`

**Removed**:

- QR parsing logic (volunteerId::eventId extraction)
- Event validation call
- Volunteer-specific fields

**Added**:

- Direct QR string passing to backend
- Wallet-based response handling
- Wallet ID and balance display
- Better logging for debugging

**New Flow**:

```typescript
// 1. Scan QR and get raw qrString
const qrString = data;

// 2. Call login endpoint (no parsing needed)
const loginResponse = await volunteerApi.loginWithQr(qrString);

// 3. Handle response with wallet info
await setVolunteerData({
  userId: userData.userId,
  walletId: userData.walletId,
  walletBalance: userData.balance,
  token: userData.token,
  refreshToken: userData.refreshToken,
  // ... other fields
});

// 4. Navigate to event screen
router.replace('/event');
```

---

## FIXING "SESSION EXPIRED" ERROR

### Root Causes:

1. **Missing tokens in response** - Solved by implementing wallet-based QR login
2. **Empty string tokens** - Now checked before setVolunteerData
3. **Token not being stored** - Ensure AsyncStorage is working
4. **JWT validation failing** - Check token format and expiration

### Validation Chain:

#### 1. Check Token Response

```typescript
if (!userData.token || !userData.refreshToken) {
  throw new Error('Missing authentication tokens');
}

if (typeof userData.token !== 'string' || userData.token.trim() === '') {
  throw new Error('Invalid token format');
}
```

#### 2. Check Token Storage (AsyncStorage)

```typescript
await AsyncStorage.setItem('auth_token', token);
await AsyncStorage.setItem('refresh_token', refreshToken);

// Verify stored
const stored = await AsyncStorage.getItem('auth_token');
if (!stored) {
  throw new Error('Failed to store token');
}
```

#### 3. Check Token Interceptor

Ensure tokens are included in ALL requests:

```typescript
// In token-refresh-interceptor.ts
const token = await AsyncStorage.getItem('auth_token');
if (token && token.trim() !== '') {
  config.headers.Authorization = `Bearer ${token}`;
}
```

#### 4. Check Token Refresh Logic

Prevent cascading refresh:

```typescript
// In proactive-token-refresh.ts
const MINIMUM_THRESHOLD = 30; // minutes
if (timeRemaining < MINIMUM_THRESHOLD) {
  // Skip refresh for very short-lived tokens
  return;
}
```

---

## DATABASE QUERIES USED

### Wallet-Based Login:

```sql
-- 1. Find wallet by QR string
SELECT * FROM wallets WHERE qr_string = ?

-- 2. Find user
SELECT * FROM users WHERE user_id = ?

-- 3. Get wallet balance
SELECT balance FROM wallets WHERE id = ?
```

### Phone-Based Login:

```sql
-- 1. Find user by phone
SELECT * FROM users WHERE phone = ?

-- 2. Find wallets for user
SELECT * FROM wallets WHERE user_id = ?

-- 3. Get wallet balance (if exists)
SELECT id, balance FROM wallets WHERE user_id = ? LIMIT 1
```

---

## TRANSACTION UPDATES NEEDED

When TopUp is performed, ensure the following are updated:

### 1. Create VolunteerWalletTransaction Record:

```java
VolunteerWalletTransaction transaction = VolunteerWalletTransaction.builder()
    .transactionId(generateId())
    .volunteerId(volunteeId)  // Who received topup
    .userId(user.getUserId())
    .walletId(wallet.getId())
    .topupUserId(topupUserId)  // ← WHO PERFORMED TOPUP (staff)
    .amount(amount)
    .paymentMethod("CASH" or "UPI")  // ← PAYMENT MODE
    .type("TOPUP_CASH" or "TOPUP_UPI")
    .status("SUCCESS")
    .timestamp(LocalDateTime.now())
    .build();
```

### 2. Update Wallet Balance:

```java
wallet.setBalance(wallet.getBalance() + amount);
walletRepository.save(wallet);
```

### 3. Update TopupBalance (if needed):

```sql
UPDATE topup_balances
SET available_balance = available_balance + ?,
    updated_at = NOW()
WHERE user_id = ? AND event_id = ?
```

### 4. Update WalletWalletTransaction (for history):

```java
WalletWalletTransaction walletTx = new WalletWalletTransaction();
walletTx.setUserId(userId);
walletTx.setEventId(eventId);
walletTx.setAmount(amount);
walletTx.setTransactionType("credit");
walletTx.setDescription("Wallet topup - " + paymentMethod + " by topup_user_" + topupUserId);
walletTx.setTransactionDate(LocalDateTime.now());
walletTransactionRepository.save(walletTx);
```

---

## TESTING THE IMPLEMENTATION

### Test 1: QR Login

```bash
POST /api/volunteer/login-qr
Content-Type: application/json

{
  "qrString": "USER_ID::WALLET_ID::TIMESTAMP_HASH"
}

Expected Response:
{
  "success": true,
  "userId": "user_123",
  "walletId": "wallet_456",
  "balance": 1500.00,
  "userName": "John User",
  "token": "eyJ...",
  "refreshToken": "eyJ..."
}
```

### Test 2: Phone Login (With Wallet)

```bash
POST /api/volunteer/login-phone
Content-Type: application/json

{
  "phone": "+919876543210"
}

Expected Response:
{
  "success": true,
  "userId": "user_123",
  "walletId": "wallet_456",
  "balance": 500.00,
  "userName": "John User",
  "token": "eyJ...",
  "refreshToken": "eyJ..."
}
```

### Test 3: Phone Login (No Wallet - First Time)

```bash
POST /api/volunteer/login-phone
{
  "phone": "+919876543210"
}

Expected Response:
{
  "success": true,
  "userId": "user_456",
  "walletId": null,
  "balance": null,
  "userName": "Jane User",
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "message": "First topup - wallet will be created"
}
```

---

## FILES MODIFIED

### Backend:

- ✅ `VolunteerService.java` - Added wallet imports, walletRepository field, loginWithQr (rewritten), loginWithPhone (new)
- ✅ `VolunteerController.java` - Updated login-qr description, added login-phone endpoint
- ✅ `VolunteerResponse.java` - Added walletId, balance, userName fields
- ✅ `QrLoginRequest.java` - Changed from volunteerId+eventId to qrString only
- ✅ `PhoneLoginRequest.java` - Created new DTO

### Frontend:

- ✅ `volunteer-request.ts` - Updated loginWithQr method signature
- ✅ `login-qr.tsx` - Removed QR parsing, uses wallet-based approach

---

## NEXT STEPS

1. **Test QR Login**: Scan QR code and verify authentication tokens are returned
2. **Test Phone Login**: Login with phone and confirm wallet details (if exists)
3. **Test Session Persistence**: Ensure tokens stay valid across app restarts
4. **Test Token Refresh**: Verify automatic token refresh works when tokens expire
5. **Monitor Logs**: Check backend and frontend logs for any "session expired" errors
6. **Update Topup Process**: Ensure topupUserId and paymentMethod are recorded in transactions

---

## DEBUGGING TIPS

If "session expired" still occurs:

1. **Check AsyncStorage**:

```typescript
const token = await AsyncStorage.getItem('auth_token');
console.log('Stored token:', token ? 'Present' : 'Missing');
```

2. **Check Interceptor**:

```typescript
// Add to request interceptor
console.log('[Interceptor] Adding token:', token?.substring(0, 20) + '...');
console.log('[Interceptor] Authorization header set:', !!token);
```

3. **Check API Response**:

```typescript
console.log('[API] Login response tokens:', {
  hasToken: !!response.data.token,
  tokenLength: response.data.token?.length,
  hasRefreshToken: !!response.data.refreshToken,
});
```

4. **Check Context**:

```typescript
// After setVolunteerData, verify values
const stored = await AsyncStorage.getItem('auth_token');
console.log('[Context] After setVolunteerData, token stored:', !!stored);
```
