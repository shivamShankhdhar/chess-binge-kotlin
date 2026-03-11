# QR Validation Quick Reference

## 🔐 Security Answer: Can Old QR Codes Be Used?

## ❌ NO - Absolutely Not

### Why?

The backend validates the **EXACT STRING MATCH** between:

- **Scanned QR** (what user scans)
- **Stored QR** (current value in database)

```java
// Location: cirkle-backend-common/src/main/java/com/razespace/cirklebackend/stall/service/RedemptionService.java
// Line 154-160

if (wallet.getQrString() == null || !wallet.getQrString().equals(qrString)) {
    throw new RuntimeException("Invalid QR code - QR string doesn't match");
}
```

---

## 📊 QR String Format & Regeneration

### Format

```
{userId}::{walletId}::{timestamp}
550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123
```

### On Regeneration

```
BEFORE:
wallet.qrString = "...::...::1708331520000"

USER CLICKS "REGENERATE QR"
↓
Backend generates new timestamp

AFTER:
wallet.qrString = "...::...::1708331520999"  ← CHANGED
```

### Old QR Becomes Invalid

```
User scans old: "...::...::1708331520000"
System checks:  "...::...::1708331520000" vs "...::...::1708331520999"
Result:         1708331520000 ≠ 1708331520999
                ↓
                REJECTED ✗
```

---

## 📱 Frontend to Backend Flow

### 1. Redemption App (Frontend)

```
User scans QR → "550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123"
                ↓
                Extract userId → "550e8400-e29b-41d4-a716-446655440000"
                ↓
                Create Payload:
                {
                  eventId: "stall-event",
                  userId: "550e8400-e29b-41d4-a716-446655440000",
                  qrString: "550e8400-e29b-41d4-a716-446655440000::660f9511-f30c-41d4-b816-667766551111::1708331520123",
                  items: [...],
                  totalAmount: 250.50,
                  token: "jwt-token"
                }
                ↓
                POST /api/v1/stalls/checkout (send payload)
```

### 2. Backend (RedemptionService)

```
Receive Payload
↓
Validate User exists
✓ User found
↓
Validate Event exists
✓ Event found
↓
Extract QR components:
- qrUserId = "550e8400-e29b-41d4-a716-446655440000"
- walletId = "660f9511-f30c-41d4-b816-667766551111"
- timestamp = "1708331520123"
↓
Find Wallet by ID
✓ Wallet found
↓
🔐 CRITICAL CHECK:
  if (!wallet.getQrString().equals(qrString))
      throw "Invalid QR code - QR string doesn't match"
↓
✓ QR String matches! Continue...
↓
Validate Wallet unlocked
✓ Not locked
↓
Validate Minimum top-up
✓ Has ₹500+
↓
Validate Balance
✓ Sufficient funds
↓
Process Payment
✓ Deduct amount
↓
Return Success Response
```

---

## 🚫 What Happens with Old QR

```
TIMELINE:

T1: 10:30 AM
├─ User: Generate QR
├─ System: wallet.qrString = "...::...::1708331520123"
└─ User: Screenshots QR

T2: 10:31 AM
├─ User: Tries payment with screenshot QR
├─ Backend: "QR string matches, payment successful"
└─ Result: ✅ Payment works

T3: 10:45 AM
├─ User: Regenerates QR in wallet app
├─ System: wallet.qrString = "...::...::1708331560789"  ← UPDATED
└─ Result: Old QR is now INVALID

T4: 11:00 AM
├─ Hacker: Tries to use old screenshot QR from T1
├─ Old QR: "...::...::1708331520123"
├─ Backend: Compares with current: "...::...::1708331560789"
├─ Result: "1708331520123" ≠ "1708331560789"
│           ↓
│           NOT EQUAL
│           ↓
│           throw RuntimeException("Invalid QR code - QR string doesn't match")
└─ Payment: ❌ REJECTED
```

---

## ✅ Validation Checklist (Order Matters)

```
Every checkout goes through this checklist:

1. ✓ User exists?
2. ✓ Event exists?
3. ✓ QR format valid? (has :: delimiter)
4. ✓ Wallet exists?
5. ✓ UserId in QR = Request userId?
6. ✓ 🔐 QR string exact match?  ← CRITICAL SECURITY CHECK
7. ✓ Wallet locked?
8. ✓ Minimum ₹500 top-up?
9. ✓ Sufficient balance?
10. ✓ Proceed with payment

If ANY check fails → Entire payment is REJECTED
```

---

## 📊 Logging Output Samples

### SUCCESS (Current Valid QR)

```
Frontend Log:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ CHECKOUT SUCCESS
Total Amount Charged: 250.50
Transaction ID: TXN-1708331520-12345
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Backend Log:
🔍 QR STRING COMPARISON
Scanned QR String: 550e...::660f...::1708331520123
Stored QR String:  550e...::660f...::1708331520123
Match Result: true
✅ QR String VALID - QR code matches stored value
```

### FAILURE (Old Regenerated QR)

```
Frontend Log:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ CHECKOUT FAILED (API Response)
Error Message: Invalid QR code - QR string doesn't match
Is QR Related Error: true
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔄 QR Error Detected - Clearing cached QR data for rescan

Backend Log:
🔍 QR STRING COMPARISON
Scanned QR String: 550e...::660f...::1708331520000  ← OLD TIMESTAMP
Stored QR String:  550e...::660f...::1708331520999  ← NEW TIMESTAMP
Match Result: false
❌ QR STRING MISMATCH - Payment REJECTED
   This could mean:
   1. QR code was regenerated and old QR is no longer valid
```

---

## 🧪 How to Test This Yourself

### Test: Old QR Should Be Rejected

**Preparation:**

1. Have cirkle-app and redemption-app running
2. User account with wallet containing balance

**Steps:**

1. **Capture Current QR**
   - Open cirkle-app → Wallet
   - Screenshot wallet QR (note timestamp in QR)
   - Example: `...::...::1708331520123`

2. **Make Payment with Current QR**
   - Open redemption-app
   - Scan the QR code from screenshot
   - Add items to cart
   - Press Checkout
   - ✅ Payment should succeed

3. **Regenerate QR**
   - Return to cirkle-app
   - Open Wallet → QR Code modal
   - Click "Generate New QR Code"
   - Confirm regeneration
   - New QR is displayed with new timestamp: `...::...::1708331520999`

4. **Try Old QR Again**
   - Open redemption-app
   - Try to scan the OLD screenshot QR
   - Add items to cart
   - Press Checkout
   - ❌ Payment should FAIL with error:
     ```
     "Invalid QR code - QR string doesn't match"
     ```

5. **Try New QR**
   - Go back to cirkle-app wallet
   - Copy/screenshot the new QR
   - Return to redemption-app
   - Scan the NEW QR
   - Add items
   - Checkout
   - ✅ Payment should succeed

---

## 🔍 Database View

```sql
-- Check what QR is currently stored for a wallet

SELECT
  id as wallet_id,
  user_id,
  event_id,
  qr_string as current_qr,
  is_wallet_locked as locked,
  balance
FROM wallet
WHERE user_id = '550e8400-e29b-41d4-a716-446655440000'
AND event_id = 'GENERAL';

RESULT:
┌─────────────────────────────────────────────────────────────────────┐
│ wallet_id                         │ user_id│ event_id│ current_qr   │
├──────────────────────────────────┼────────┼───────┼──────────────┤
│ 660f9511-f30c-41d4-b816-667766.. │ 550e.. │GENERAL│ 550e..::660f │
│                                  │        │       │ ..::17083315 │
│                                  │        │       │ 20999        │
└─────────────────────────────────────────────────────────────────────┘

This qr_string value is what the backend checks against!
If scanned QR doesn't match this EXACTLY → REJECTED
```

---

## 🎯 Key Takeaway

| Aspect                          | Details                                                       |
| ------------------------------- | ------------------------------------------------------------- |
| **What is validated?**          | The ENTIRE QR string (byte-by-byte exact match)               |
| **Where is it validated?**      | Backend: RedemptionService.java line 154                      |
| **Can old QRs be used?**        | NO - String comparison fails                                  |
| **What happens on mismatch?**   | RuntimeException: "Invalid QR code - QR string doesn't match" |
| **User experience on failure?** | "Please scan your current QR code" message                    |
| **How to prevent abuse?**       | Regenerate QR whenever security concern                       |
| **Logging enabled?**            | YES - All validation steps logged                             |

---

## 🔗 File References

| File                       | Location                                      | Purpose                        |
| -------------------------- | --------------------------------------------- | ------------------------------ |
| **RedemptionService.java** | cirkle-backend-common/src/.../stall/service/  | QR validation logic            |
| **order.tsx**              | redemption-app/src/app/(root)/                | Frontend QR scanning & logging |
| **wallet-screen.tsx**      | cirkle-app/src/screens/                       | QR regeneration                |
| **WalletQrService.java**   | cirkle-backend-common/src/.../wallet/service/ | QR generation & utilities      |

---

## ✅ Conclusion

**Old QR codes cannot be used after regeneration because:**

1. ✓ Each QR contains unique timestamp
2. ✓ Regeneration changes timestamp
3. ✓ Backend compares ENTIRE string character-by-character
4. ✓ Any difference (even 1 character) causes rejection
5. ✓ Old timestamp ≠ new timestamp → Payment denied
6. ✓ User must use current QR from wallet app

This is not a vulnerability - it's an intentional **security feature**.
