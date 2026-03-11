# QR Validation Logging Guide

## 📱 Frontend Logging (Redemption App)

### How to View Logs

1. **Android Device**
   - Connect Android device via USB
   - Open Android Studio Logcat
   - Filter by package name: `redemption.app` or search term

2. **Chrome DevTools (Web)**
   - Press F12 → Console tab
   - All console.log statements visible in real-time

3. **React Native Debugger**
   - Run app with debugger attached
   - View console output

### Log Markers to Search For

#### 📱 QR CODE SCANNED

```
Search term: "📱 QR CODE SCANNED"

Output shows:
- Raw QR Data (complete string)
- QR Length
- Whether it contains :: delimiter
- QR Parts breakdown:
  - [0] UserId
  - [1] WalletId
  - [2] Timestamp
```

**Example Output:**

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

#### 📤 CHECKOUT PAYLOAD SENDING TO SERVER

```
Search term: "📤 CHECKOUT PAYLOAD"

Output shows:
- Event ID being used
- User ID extracted from QR
- Complete QR String that will be validated
- Items being purchased (count and detail)
- Total Amount
- Token status
- HTTP timestamp

This is the EXACT payload being sent to backend!
```

**Example Output:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📤 CHECKOUT PAYLOAD SENDING TO SERVER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Event ID: event-2024-carnival
User ID: 550e8400-e29b-41d4-a716-446655440000
QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Items Count: 2
Items: [
  { "itemId": "item-pizza", "quantity": 2 },
  { "itemId": "item-coke", "quantity": 1 }
]
Total Amount: 450.00
Token: ✓ Present
Payload Timestamp: 2024-02-18T10:30:45.123Z
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### ✅ CHECKOUT SUCCESS

```
Search term: "✅ CHECKOUT SUCCESS"

Output shows:
- Transaction ID (proof of successful payment)
- Amount charged
- Items processed
- Full response from server

This confirms payment went through!
```

**Example Output:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ CHECKOUT SUCCESS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Transaction ID: TXN-1708331520-12345
Total Amount Charged: 450.00
Items Processed: 2
Response: {
  "success": true,
  "message": "Checkout successful",
  "transactionId": "TXN-1708331520-12345",
  "totalAmount": 450.00,
  "items": [
    {"itemId": "item-pizza", "quantity": 2, "unitPrice": 200, "totalPrice": 400},
    {"itemId": "item-coke", "quantity": 1, "unitPrice": 50, "totalPrice": 50}
  ]
}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### ❌ CHECKOUT FAILED (API Response)

```
Search term: "❌ CHECKOUT FAILED (API Response)"

Output shows:
- Error message from backend
- Whether it's QR-related
- Full error response

If "Is QR Related Error: true" → User sees "🔄 QR Error Detected"
```

**Example Output (Invalid QR):**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ CHECKOUT FAILED (API Response)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Error Message: Invalid QR code - QR string doesn't match
Is QR Related Error: true
Full Response: {
  "success": false,
  "message": "Invalid QR code - QR string doesn't match"
}
Timestamp: 2024-02-18T10:35:12
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 QR Error Detected - Clearing cached QR data for rescan
```

#### ❌ CHECKOUT FAILED (Exception)

```
Search term: "❌ CHECKOUT FAILED (Exception)"

Output shows:
- Exception type
- Error message
- HTTP status code
- Full error stack
- Whether it's QR-related

Network errors, validation errors, etc. appear here.
```

---

## 🖥️ Backend Logging (Spring Boot)

### How to View Logs

1. **Docker Container Logs**

   ```bash
   docker logs -f cirkle-backend-common
   ```

2. **Kubernetes Logs**

   ```bash
   kubectl logs -f deployment/cirkle-backend
   ```

3. **Local Development**
   - Application output in IDE console
   - Or tail the log file: `tail -f logs/application.log`

4. **AWS CloudWatch**
   - Log group: `/aws/ecs/cirkle-backend`
   - Search for log streams

### Log Markers to Search For

#### 📥 CHECKOUT REQUEST RECEIVED

```
Search term: "📥 CHECKOUT REQUEST RECEIVED"

Shows when request arrives at backend:
- Event ID
- User ID
- QR String (the one being validated)
- Total Amount
- Items Count
- Request timestamp

This is the START of backend processing.
```

**Example Output:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📥 CHECKOUT REQUEST RECEIVED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Event ID: event-2024-carnival
User ID: 550e8400-e29b-41d4-a716-446655440000
QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Total Amount: ₹450.00
Items Count: 2
Request Timestamp: 2024-02-18T10:30:45
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### 🔐 QR CODE VALIDATION STARTED

```
Search term: "🔐 QR CODE VALIDATION STARTED"

Shows the QR validation phase:
- Raw QR string received
- Parsed QR parts
- User ID from QR
- Wallet ID from QR
- Timestamp from QR

This is the CRITICAL security checkpoint.
```

**Example Output:**

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
```

#### 🔍 QR STRING COMPARISON

```
Search term: "🔍 QR STRING COMPARISON"

The MOST IMPORTANT log!

Shows:
- Scanned QR String (from user's scan)
- Stored QR String (current value in database)
- Match Result: true/false

THIS IS WHERE OLD QRS GET REJECTED!
```

**Example Output (CURRENT QR - SUCCESS):**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 QR STRING COMPARISON
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Scanned QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Stored QR String:  550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Match Result: true
✅ QR String VALID - QR code matches stored value
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Example Output (OLD QR - REJECTED):**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 QR STRING COMPARISON
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Scanned QR String: 550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520000
Stored QR String:  550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
Match Result: false
❌ QR STRING MISMATCH - Payment REJECTED
   This could mean:
   1. QR code was regenerated and old QR is no longer valid
   2. QR code belongs to different wallet
   3. QR code is corrupted or invalid
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Notice the timestamps:

- Scanned: `1708331520000` (OLD)
- Stored: `1708331520123` (NEW)
- These don't match → Rejected ✗

#### 🎯 WALLET QR VALIDATION COMPLETE

```
Search term: "🎯 WALLET QR VALIDATION COMPLETE"

All QR checks passed!

Means:
- QR string matches ✓
- Wallet unlocked ✓
- Minimum top-up done ✓
- Sufficient balance ✓

Now proceeding with checkout...
```

---

## 🧪 Test Scenarios with Expected Logs

### Scenario 1: Valid Payment with Current QR

**Steps:**

1. Generate QR in cirkle-app
2. Go to redemption app
3. Scan QR code
4. Add items to cart
5. Click checkout

**Expected Logs:**

Frontend:

```
📱 QR CODE SCANNED
→ Shows: QR timestamp = 1708331520123

📤 CHECKOUT PAYLOAD SENDING TO SERVER
→ Shows: qrString = "...::...::1708331520123"

✅ CHECKOUT SUCCESS
→ Shows: Transaction ID, amount charged
```

Backend:

```
📥 CHECKOUT REQUEST RECEIVED
→ Shows: QR string received

🔐 QR CODE VALIDATION STARTED
→ Shows: QR parts parsed correctly

🔍 QR STRING COMPARISON
→ Shows: Match Result: true ✓

🎯 WALLET QR VALIDATION COMPLETE
→ Shows: All checks passed
```

---

### Scenario 2: Attempted Payment with OLD QR (After Regeneration)

**Steps:**

1. Save/screenshot QR 1 (timestamp: 1708331520123)
2. Go to wallet → Regenerate QR (creates QR 2 with timestamp: 1708331520999)
3. Go to redemption app
4. Try to scan OLD saved QR 1
5. Click checkout

**Expected Logs:**

Frontend:

```
📱 QR CODE SCANNED
→ Shows: QR timestamp = 1708331520123 (OLD)

📤 CHECKOUT PAYLOAD SENDING TO SERVER
→ Shows: qrString = "...::...::1708331520123"

❌ CHECKOUT FAILED (API Response)
→ Shows: "Invalid QR code - QR string doesn't match"
→ Shows: "Is QR Related Error: true"

🔄 QR Error Detected - Clearing cached QR data for rescan
```

Backend:

```
📥 CHECKOUT REQUEST RECEIVED
→ Shows: OLD QR string received

🔐 QR CODE VALIDATION STARTED
→ Shows: QR parts parsed

🔍 QR STRING COMPARISON
→ Shows:
   Scanned: "...::...::1708331520123" (OLD)
   Stored: "...::...::1708331520999" (NEW)
   Match Result: false ✗

❌ QR STRING MISMATCH - Payment REJECTED
   → This could mean: QR code was regenerated and old QR is no longer valid
```

---

### Scenario 3: Payment with Locked Wallet

**Steps:**

1. Lock wallet in cirkle-app
2. Go to redemption app
3. Scan QR code
4. Add items
5. Click checkout

**Expected Logs:**

Frontend & Backend:

```
QR Validation: PASSES ✓
→ "✅ QR String VALID"

Lock Status Check: FAILS ✗
→ "❌ GENERAL WALLET LOCKED"
→ "Wallet is locked - no deductions allowed"

Response to frontend:
"Wallet is locked - no deductions can be made"
```

---

## 🎯 Key Things to Verify

### ✅ Old QR is Blocked

1. Frontend log shows: Scanned QR timestamp (OLD)
2. Backend log shows:
   - "Scanned QR String: ...::...::TIMESTAMP_OLD"
   - "Stored QR String: ...::...::TIMESTAMP_NEW"
   - "Match Result: false"
   - "❌ QR STRING MISMATCH"
3. Error message appears: "Invalid QR code - QR string doesn't match"

### ✅ New QR Works

1. Frontend log shows: Scanned QR timestamp (CURRENT)
2. Backend log shows:
   - "Match Result: true"
   - "✅ QR String VALID"
   - "🎯 WALLET QR VALIDATION COMPLETE"
3. Success message appears with transaction ID

### ✅ Payloads Are Correct

1. Frontend shows complete payload being sent:
   - Event ID
   - User ID
   - QR String
   - Items with quantities
   - Total amount
2. Backend shows exactly what was received

---

## 📋 Logging Performance Impact

These logs use `@Slf4j` (SLF4J) which is:

- **Non-blocking** - minimal performance impact
- **Conditional** - only active at configured log level
- **Filtered** - can be disabled for production optimization

### Disabling Detailed Logs (Production)

In `application.properties`:

```properties
# Change from DEBUG to INFO
logging.level.com.razespace.cirklebackend=INFO
logging.level.com.razespace.cirklebackend.stall.service=WARN
```

---
