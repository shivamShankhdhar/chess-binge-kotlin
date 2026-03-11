# CORRECTED QR & TOPUP USER LOGIN IMPLEMENTATION

## Overview

The QR login and topup user phone login processes work with **wallets**, not volunteer records. This document outlines the correct architecture and implementation.

---

## 1. QR LOGIN PROCESS (Wallet-Based)

### Flow:

```
User scans QR code
    ↓
Extract qrString from QR code (raw string, not parsed)
    ↓
Search wallet table by qrString
    ↓
Get userId from wallet
    ↓
Verify user exists in users DB
    ↓
Get walletId and current balance
    ↓
Return: { success, userId, walletId, balance, userName, eventId }
```

### Backend Endpoint: `POST /api/volunteer/login-qr` (NEW)

**Request Body:**

```json
{
  "qrString": "USER_ID::WALLET_ID::TIMESTAMP_HASH"
}
```

**Response:**

```json
{
  "success": true,
  "userId": "user_123",
  "walletId": "wallet_456",
  "balance": 1500.0,
  "userName": "John Volunteer",
  "phone": "+919876543210",
  "email": "john@example.com",
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "message": "QR login successful"
}
```

### Database Queries:

1. `walletRepository.findByQrString(qrString)` → Returns Wallet entity
2. `userRepository.findById(wallet.getUserId())` → Returns User entity
3. Get balance from `wallet.getBalance()`

---

## 2. PHONE LOGIN PROCESS (TopUp User - Wallet-Based)

### Flow:

```
TopUp User enters phone number
    ↓
Search user by phone
    ↓
Check if user exists
    ↓
Search for wallet in wallet table
    ↓
If wallet exists: Return userId, walletId, balance
If wallet NOT exists: Return userId with null for walletId and balance
    ↓
Response: { success, userId, walletId (optional), balance (optional), userName, phone }
```

### Backend Endpoint: `POST /api/volunteer/login-phone` (NEW)

**Request Body:**

```json
{
  "phone": "+919876543210"
}
```

**Response - When wallet exists:**

```json
{
  "success": true,
  "userId": "user_123",
  "walletId": "wallet_456",
  "balance": 500.0,
  "userName": "John Volunteer",
  "phone": "+919876543210",
  "message": "Login successful"
}
```

**Response - When wallet doesn't exist:**

```json
{
  "success": true,
  "userId": "user_123",
  "walletId": null,
  "balance": null,
  "eventId": null,
  "userName": "John Volunteer",
  "phone": "+919876543210",
  "message": "First topup - wallet will be created"
}
```

### Database Queries:

1. `userRepository.findByPhone(phone)` → Returns User entity
2. `walletRepository.findByUserId(userId)` → Returns optional Wallet
3. If not found, return null for wallet-related fields

---

## 3. WALLET TOPUP TRANSACTION DETAILS

### When Topup is Done (by topup_user or volunteer themselves):

**Include in Transaction Record:**

- `transactionId`: Unique transaction ID
- `walletId`: Which wallet was topped up
- `userId`: The user whose wallet was topped up
- `topupUserId`: WHO did the topup (topup_user_id if done by staff, or userId if self-topup)
- `amount`: Topup amount
- `paymentMethod`: "CASH" or "UPI" or "RAZORPAY"
- `paymentStatus`: "SUCCESS" or "PENDING" or "FAILED"
- `timestamp`: When topup happened
- `eventId`: Which event this topup is for
- `description`: Details about the transaction

### Transaction Tables to Update:

#### 1. `VolunteerWalletTransaction` (For volunteer wallet topups):

```java
VolunteerWalletTransaction {
    transactionId: "TXN_123",
    volunteerId: "vol_456",
    userId: "user_789",
    walletId: "wallet_111",
    topupUserId: "topup_user_222",  // ← Staff member who processed it
    eventId: "event_333",
    type: "TOPUP_CASH" or "TOPUP_UPI",
    amount: 500.00,
    paymentMethod: "CASH" or "UPI",  // ← Payment mode
    paymentStatus: "SUCCESS",
    razorpayOrderId: "order_444" (if UPI),
    razorpayPaymentId: "pay_555" (if UPI),
    description: "Topup via CASH by staff",
    status: "SUCCESS",
    timestamp: 2026-02-22T10:30:00
}
```

#### 2. `TopupBalance` (Balance tracking for topups):

```sql
UPDATE topup_balances
SET available_balance = available_balance + 500.00,
    updated_at = NOW()
WHERE user_id = 'user_789' AND event_id = 'event_333'
```

If record doesn't exist:

```sql
INSERT INTO topup_balances (user_id, event_id, available_balance, used_balance, created_at)
VALUES ('user_789', 'event_333', 500.00, 0.0, NOW())
```

#### 3. `WalletWalletTransaction` (General transaction history):

```
userId: "user_789"
eventId: "event_333"
amount: 500.00
transactionType: "credit"
description: "Wallet topup - CASH by staff topup_user_222"
transactionDate: 2026-02-22T10:30:00
```

---

## 4. FRONTEND FLOW (Volunteer App - Recharge Wallet)

### Current Process in `recharge-wallet.tsx`:

1. Topup User selects volunteer
2. Enters amount
3. Chooses payment method (CASH or UPI)
4. If UPI: Use Razorpay
5. If CASH: Direct verification (no payment gateway)

### When Topup API Response Received:

```typescript
const response = await volunteerApi.topupWalletCash(
  volunteerId,
  eventId,
  amountNum,
  paymentMethod,  // "CASH" or "UPI"
  topupUserId,    // Who performed the topup
  razorpayOrderId, // If UPI
  razorpayPaymentId // If UPI
);

// Response should include:
{
  success: true,
  walletBalance: 1500.00,
  transactionId: "TXN_123",
  walletId: "wallet_456",
  message: "Topup successful"
}
```

### Update Local UI:

```typescript
setSelectedVolunteer({
  ...selectedVolunteer,
  wallet: {
    walletId: response.walletId,
    balance: response.walletBalance,
  },
});
```

---

## 5. CORRESPONDING BACKEND CHANGES NEEDED

### New DTOs:

#### `QrLoginRequest.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrLoginRequest {
    private String qrString;  // Raw QR string from scanned QR
}
```

#### `PhoneLoginRequest.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneLoginRequest {
    private String phone;  // Phone number with +91 prefix
}
```

#### `TopupWalletRequest.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopupWalletRequest {
    private String volunteerId;      // Volunteer being topped up
    private String eventId;          // Event context
    private Double amount;           // Topup amount
    private String paymentMethod;    // "CASH" or "UPI"
    private String topupUserId;      // Staff member doing topup
    private String razorpayOrderId;  // If UPI payment
    private String razorpayPaymentId; // If UPI payment
}
```

---

## 6. DEPENDENT TABLES & RELATIONSHIPS

```
┌──────────────────┐
│   Wallet         │
├──────────────────┤
│ id (PK)         │
│ userId (FK) ────┼────┐
│ eventId (FK)    │    │
│ balance         │    │
│ qrString ◄──────┼────┼── QR Login searches here
│ isWalletLocked  │    │
└──────────────────┘    │
                        │
                    ┌────────────────┐
                    │  User          │
                    ├────────────────┤
                    │ userId (PK)    │
                    │ phone  ◄─────┐ │
                    │ name         │ │
                    │ email        │ │
                    │ authMethod   │ │
                    └────────────────┘
                        │
                        │
    ┌───────────────────┴──────────────────┐
    │                                      │
    ▼                                      ▼
┌──────────────────────────┐  ┌─────────────────────┐
│ TopupBalance             │  │ TopupTransaction    │
├──────────────────────────┤  ├─────────────────────┤
│ id                       │  │ transactionId       │
│ userId (FK) ────┐        │  │ userId (FK) ────┐   │
│ eventId         │        │  │ walletId (FK)   │   │
│ availableBalance│        │  │ topupUserId ◄───┼── │ Staff who did topup
│ usedBalance     │        │  │ amount          │   │
│ createdAt       │        │  │ paymentMethod   │   │
│ updatedAt       │        │  │ paymentStatus   │   │
└──────────────────────────┘  └─────────────────────┘
                                                  ▲
                                                  │
    ┌─────────────────────────────────────────────┘
    │
    ▼
┌──────────────────────────┐
│ WalletWalletTransaction  │
├──────────────────────────┤
│ transactionId            │
│ userId (FK)              │
│ amount                   │
│ transactionType (credit) │
│ description              │
│ transactionDate          │
└──────────────────────────┘
```

---

## 7. SOLVING "SESSION EXPIRED" ERROR

The "session expired" error likely occurs because:

1. **Empty tokens are being set**: When login response is missing tokens
2. **Token refresh loop**: When tokens are too short-lived
3. **JWT validation issue**: When tokens aren't properly validated

### Check Points:

- ✅ Ensure login endpoint returns valid JWT tokens
- ✅ Ensure tokens have proper expiration (15 min access, 7 day refresh)
- ✅ Ensure AsyncStorage is properly storing non-empty tokens
- ✅ Ensure token refresh interceptor only runs after successful login

### Solutions Already Implemented:

- ✅ Added token validation before setting in context
- ✅ Skip proactive refresh for short-lived tokens
- ✅ Filter refresh request for login-only endpoints
- ✅ Log tokens at every step

### Additional Check:

When setting volunteer data in context, ensure:

```typescript
if (!token || !refreshToken) {
  throw new Error('Missing authentication tokens');
}

await AsyncStorage.setItem('auth_token', token);
await AsyncStorage.setItem('refresh_token', refreshToken);
```

---

## 8. IMPLEMENTATION SUMMARY

| Component         | Current               | Fix Needed                     |
| ----------------- | --------------------- | ------------------------------ |
| QR Login          | Uses volunteer ID     | Use wallet qrString search ✓   |
| Phone Login       | Not implemented       | Implement with phone search ✓  |
| Topup Transaction | Missing topupUserId   | Add topup_user_id field ✓      |
| Payment Mode      | Not specified         | Add payment method tracking ✓  |
| Wallet Lookup     | Not optimized         | Index on qrString ✓            |
| First Topup       | Assumes wallet exists | Create wallet on first topup ✓ |
| Session Expired   | Token issues          | Validate all entry points ✓    |

---

## 9. Code Change Checklist

### Backend:

- [ ] Create `QrLoginRequest` DTO
- [ ] Create `PhoneLoginRequest` DTO
- [ ] Create `POST /api/volunteer/login-qr` endpoint
- [ ] Create `POST /api/volunteer/login-phone` endpoint
- [ ] Add `topupUserId` field to `VolunteerWalletTransaction`
- [ ] Update topup endpoints to accept and store payment method & topupUserId
- [ ] Ensure `walletRepository.findByQrString()` exists
- [ ] Ensure `userRepository.findByPhone()` exists
- [ ] Update transaction creation to store all details

### Frontend:

- [ ] Replace `volunteerApi.getVolunteerDetails()` with `volunteerApi.loginWithQr()`
- [ ] Add `volunteerApi.loginWithPhone()` method
- [ ] Update `login-qr.tsx` to use wallet-based search
- [ ] Add phone login screen for topup users
- [ ] Update topup payload to include `topupUserId` and `paymentMethod`
- [ ] Validate tokens before setting in context
- [ ] Add comprehensive logging for session expired debugging
