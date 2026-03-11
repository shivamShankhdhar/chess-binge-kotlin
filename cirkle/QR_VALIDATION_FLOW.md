# QR Code Validation Flow - Complete Security Analysis

## Overview

This document explains how the wallet QR code validation prevents old regenerated QR codes from being used for payments. The system uses a **strict string matching** approach to ensure only the current QR code can be used.

---

## QR Code Format

```
Format: {userId}::{walletId}::{timestamp}
Example: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
```

### Components:

- **UserId** (UUID): The user who owns the wallet
- **WalletId** (UUID): Unique identifier for the wallet
- **Timestamp** (Unix milliseconds): When the QR was generated (changes on regeneration)

---

## Complete QR Validation Flow

### 1️⃣ FRONTEND FLOW (redemption-app)

#### Step 1: QR Code Scanning

```
User Action: Scan QR code with camera
↓
CameraView reads barcode data
↓
handleBarCodeScanned({ data: string }) called
↓
data = "550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123"
```

**LOGGING OUTPUT:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📱 QR CODE SCANNED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Raw QR Data: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
QR Length: 76
Contains "::" delimiter: true
QR Parts Count: 3
  [0] UserId: 550e8400-e29b-41d4-a716-446655440000
  [1] WalletId: 660f9511-f30c-41d4-b816-667766551111
  [2] Timestamp: 1708331520123
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ UserId Extracted: 550e8400-e29b-41d4-a716-446655440000
```

#### Step 2: Extract UserId & Validate

```
extractUserIdFromQr(data) → userId
↓
Validate userId is not null
↓
Validate eventId is available from stall context
```

#### Step 3: Build Checkout Payload

```typescript
const payload = {
  eventId: 'stall-event-id',
  userId: '550e8400-e29b-41d4-a716-446655440000',
  qrString:
    '550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123',
  items: [
    { itemId: 'item-1', quantity: 2 },
    { itemId: 'item-2', quantity: 1 },
  ],
  totalAmount: 250.5,
  token: 'jwt-token',
};
```

**LOGGING OUTPUT:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📤 CHECKOUT PAYLOAD SENDING TO SERVER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Event ID: stall-event-id
User ID: 550e8400-e29b-41d4-a716-446655440000
QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Items Count: 2
Items: [
  { "itemId": "item-1", "quantity": 2 },
  { "itemId": "item-2", "quantity": 1 }
]
Total Amount: 250.50
Token: ✓ Present
Payload Timestamp: 2024-02-18T10:30:45.123Z
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### Step 4: Send to Backend

```
POST /api/v1/stalls/{eventId}/checkout
Body: payload (JSON)
↓
Waiting for server response...
```

---

### 2️⃣ BACKEND VALIDATION FLOW (RedemptionService.java)

#### Validation Step 1: Request Logging

```
REQUEST RECEIVED on backend
↓
Log: Event ID, User ID, QR String, Total Amount, Items Count
```

**LOGGING OUTPUT:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📥 CHECKOUT REQUEST RECEIVED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Event ID: stall-event-id
User ID: 550e8400-e29b-41d4-a716-446655440000
QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Total Amount: ₹250.50
Items Count: 2
Request Timestamp: 2024-02-18T10:30:45
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### Validation Step 2: User Validation

```java
Optional<User> userOpt = userRepository.findById(request.getUserId());
if (userOpt.isEmpty()) {
    throw new RuntimeException("QR is invalid - user not found");
}
```

**Result:** ✅ User validated

#### Validation Step 3: Event Validation

```java
Optional<Event> eventOpt = eventRepository.findByEventId(request.getEventId());
if (eventOpt.isEmpty()) {
    throw new RuntimeException("You are not authorized for redemption...");
}
```

**Result:** ✅ Event validated

#### Validation Step 4: 🔐 QR STRING VALIDATION (CRITICAL)

This is the **SECURITY CHECKPOINT** that prevents old QR codes from working.

```java
if (request.getQrString() != null && !request.getQrString().isEmpty()) {
    String qrString = request.getQrString();
    String[] qrParts = qrString.split("::");

    // Parse QR components
    String qrUserId = qrParts[0].trim();      // "550e8400-e29b-41d4-a716-446655440000"
    String walletIdFromQr = qrParts[1].trim(); // "660f9511-f30c-41d4-b816-667766551111"
    String qrTimestamp = qrParts[2].trim();    // "1708331520123"

    // Get wallet from database
    Optional<Wallet> walletOpt = walletRepository.findById(walletIdFromQr);
    Wallet wallet = walletOpt.get();

    // ===== CRITICAL CHECK =====
    // Compare scanned QR string with CURRENT stored QR string in database
    if (!wallet.getQrString().equals(qrString)) {
        throw new RuntimeException("Invalid QR code - QR string doesn't match");
    }
}
```

**LOGGING OUTPUT:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔐 QR CODE VALIDATION STARTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
QR String (Raw): 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
QR Parts Count: 3
QR UserId: 550e8400-e29b-41d4-a716-446655440000
QR WalletId: 660f9511-f30c-41d4-b816-667766551111
QR Timestamp: 1708331520123
✅ User ID matches

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 QR STRING COMPARISON
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Scanned QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Stored QR String:  550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Match Result: true
✅ QR String VALID - QR code matches stored value
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### Validation Step 5: Additional Checks

```java
// Check wallet lock status
if (wallet.getIsWalletLocked()) {
    throw new RuntimeException("Wallet is locked - no deductions...");
}

// Check minimum top-up requirement
if (totalTop-up < 500) {
    throw new RuntimeException("Minimum ₹500 top-up required...");
}

// Check balance
if (wallet.getBalance() < totalAmount) {
    throw new RuntimeException("Insufficient balance...");
}
```

#### Validation Step 6: Checkout Processing

```
✅ All validations passed
↓
Deduct amount from wallet
↓
Create transaction record
↓
Generate checkout response
↓
Return success response to client
```

---

## 🔒 Security Mechanism Explained

### How OLD QR Codes Are Invalidated

#### Scenario: User Regenerates QR Code

```
1️⃣ BEFORE REGENERATION
   ┌─────────────────────────────────────┐
   │ Wallet (Database)                   │
   │ ─────────────────────────────────── │
   │ qrString: "userId::walletId::TIME1" │
   │ balance: 5000                       │
   └─────────────────────────────────────┘

   User has scanned and saved this QR ↓

2️⃣ USER CLICKS "REGENERATE QR"
   Frontend request:
   POST /api/wallet/{userId}/GENERAL/regenerate-qr

   Backend: WalletQrService.regenerateQrString()
   - Generate new QR with current timestamp
   - NEW QR: "userId::walletId::TIME2"
   - Update Wallet: qrString = "userId::walletId::TIME2"

3️⃣ AFTER REGENERATION
   ┌─────────────────────────────────────┐
   │ Wallet (Database) - UPDATED         │
   │ ─────────────────────────────────── │
   │ qrString: "userId::walletId::TIME2" │ ← CHANGED!
   │ balance: 5000                       │
   └─────────────────────────────────────┘

4️⃣ OLD QR SCAN ATTEMPT
   Scanned QR: "userId::walletId::TIME1"
   Stored QR:  "userId::walletId::TIME2"

   if (scannedQR.equals(storedQR)) {
       // STRING COMPARISON - EXACT MATCH REQUIRED
       // "userId::walletId::TIME1" != "userId::walletId::TIME2"
       // ↓
       // FAILS - throws exception
       throw new RuntimeException(
           "Invalid QR code - QR string doesn't match"
       );
   }

5️⃣ PAYMENT REJECTED
   Redemption app receives error:
   "Invalid QR code - QR string doesn't match"
   ↓
   User must scan NEW QR code
```

---

## 📊 Success Cases vs Failure Cases

### ✅ SUCCESS CASE: Valid Current QR

```
Scenario: User scans CURRENT valid QR
├── QR scanned: "userId::walletId::TIMESTAMP_NOW"
├── Backend lookup: Wallet found with qrString = "userId::walletId::TIMESTAMP_NOW"
├── String comparison: MATCHES ✓
├── Lock check: Wallet unlocked ✓
├── Balance check: Sufficient funds ✓
└── Result: PAYMENT SUCCESS ✓

RESPONSE (200 OK):
{
  "success": true,
  "message": "Checkout successful",
  "transactionId": "TXN123456",
  "totalAmount": 250.50
}
```

### ❌ FAILURE CASE 1: Old Regenerated QR

```
Scenario: User scans OLD QR (after regeneration)
├── QR scanned: "userId::walletId::TIMESTAMP_OLD"
├── Backend lookup: Wallet found
├── Current qrString in DB: "userId::walletId::TIMESTAMP_NEW"
├── String comparison:
│   TIMESTAMP_OLD ≠ TIMESTAMP_NEW
│   → MISMATCH ✗
└── Result: PAYMENT REJECTED ✗

RESPONSE (400 Bad Request):
{
  "success": false,
  "message": "Invalid QR code - QR string doesn't match"
}

REDEMPTION APP:
└── Creates error: "🔄 QR Error Detected - Clearing cached QR data for rescan"
└── Prompts user to scan AGAIN with current QR
```

### ❌ FAILURE CASE 2: Wallet Locked

```
Scenario: User tries to pay but wallet is locked
├── QR validation: PASSES ✓
├── Lock status check: isWalletLocked = true ✓
└── Result: PAYMENT REJECTED ✗

RESPONSE (400 Bad Request):
{
  "success": false,
  "message": "Wallet is locked - no deductions can be made"
}
```

### ❌ FAILURE CASE 3: Insufficient Balance

```
Scenario: User tries to pay ₹500 but only has ₹200
├── QR validation: PASSES ✓
├── Lock check: PASSES ✓
├── Balance check: 200 < 500 → FAILS ✗
└── Result: PAYMENT REJECTED ✗

RESPONSE (400 Bad Request):
{
  "success": false,
  "message": "Insufficient wallet balance. Available: ₹200.00, Required: ₹500.00"
}
```

---

## 🧪 How to Verify Old QR is Blocked

### Test Steps:

1. **Generate QR Code**
   - User generates wallet QR in cirkle-app
   - Screenshot or save the QR code display
   - Note the timestamp in QR (last part after ::)

2. **Use QR for Payment**
   - Go to redemption app
   - Scan the QR code
   - Complete checkout
   - ✅ Should succeed with current QR

3. **Regenerate QR in Wallet**
   - Go back to cirkle-app
   - Open wallet
   - Click "Generate New QR Code"
   - Confirm regeneration
   - New QR is generated with new timestamp

4. **Try Old QR Again**
   - Go to redemption app
   - Try to scan the OLD saved/screenshotted QR
   - Notice: Timestamp is different from current QR
   - Try to checkout
   - ❌ Should FAIL with error: "Invalid QR code - QR string doesn't match"

5. **Try New QR**
   - Show the new QR from wallet app
   - Scan with redemption app
   - ✅ Should succeed

---

## 📋 QR Validation Checklist

On **EVERY CHECKOUT ATTEMPT**, the backend validates:

```
CheckList:
├─ [1] User exists? ✓
├─ [2] Event exists? ✓
├─ [3] QR format valid (has ::)? ✓
├─ [4] Wallet exists? ✓
├─ [5] UserId in QR matches request UserId? ✓
├─ [6] 🔐 QR string EXACT match with stored? ✓ ← CRITICAL
├─ [7] Wallet locked? ✓
├─ [8] Minimum ₹500 top-up done? ✓
├─ [9] Sufficient balance? ✓
└─ [10] Proceed with payment ✓
```

**Any single failure = Payment rejected**

---

## 📝 Logging Locations

To debug QR validation issues, check these logs:

### Frontend (redemption-app/order.tsx)

- **QR Scan**: Search logs for `📱 QR CODE SCANNED`
- **Payload**: Search logs for `📤 CHECKOUT PAYLOAD SENDING TO SERVER`
- **Success**: Search logs for `✅ CHECKOUT SUCCESS`
- **Error**: Search logs for `❌ CHECKOUT FAILED`

### Backend (RedemptionService.java)

- **Request Received**: Search logs for `📥 CHECKOUT REQUEST RECEIVED`
- **QR Validation**: Search logs for `🔐 QR CODE VALIDATION STARTED`
- **String Compare**: Search logs for `🔍 QR STRING COMPARISON`
- **Success**: Search logs for `🎯 WALLET QR VALIDATION COMPLETE`

---

## 🎯 Bottom Line

**Old QR codes CANNOT be used after regeneration because:**

1. Each QR contains a **unique timestamp**
2. When QR is regenerated, the **timestamp changes**
3. Backend **compares the entire QR string** byte-by-byte
4. Even a 1-character difference (timestamp) causes the check to fail
5. Payment is **rejected** if strings don't match exactly
6. User must use the **current QR** from their wallet app

This is a **critical security feature** that prevents:

- Using stolen/copied old QRs
- Payment after wallet regeneration
- Unauthorized access with outdated credentials
