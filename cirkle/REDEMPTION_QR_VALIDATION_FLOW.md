# Redemption App - QR Validation & Checkout Flow

## Overview

Complete redesign of the QR validation and checkout flow for the redemption app with step-by-step validation, clear error messages, and proper loading states.

---

## Backend Flow (WalletController.java)

### Endpoint: POST `/api/wallet/validate-qr`

**Security Requirements:**

- JWT Token required
- STALL_VENDOR role required
- Token passed in Authorization header: `Bearer {token}`

**Request Body:**

```json
{
  "qrString": "userId::walletId::timestamp"
}
```

### Validation Steps:

#### STEP 1: Search Wallet by QR String

- Search wallet using `walletRepository.findByQrString(qrString)`
- **If found:** ✓ Continue to Step 2
- **If not found:** ❌ Return error 404 - "QR not valid"

#### STEP 2: Extract User ID from Wallet

- Get userIdFromWallet directly from wallet object (NOT by parsing QR string)
- **If successfully extracted:** ✓ Continue to Step 3
- **If null or empty:** ❌ Return error 400 - "Wallet data is corrupted"

#### STEP 3: Verify User Exists

- Search user using `userRepository.findById(userIdFromWallet)`
- **If found:** ✓ Continue to Step 4
- **If not found:** ❌ Return error 404 - "User not found - QR not valid"

#### STEP 4: Check Wallet Lock Status

- Check `wallet.getIsWalletLocked()`
- **If unlocked:** ✓ Continue to Step 5
- **If locked:** ❌ Return error 423 - "Wallet is locked. Unlock the wallet from the app first and then proceed with the payment"

#### STEP 5: Verify Transaction History (Optional)

- Check if user has valid transactions for the event
- Looking for transactions with amount >= ₹500
- **If valid transaction exists:** ✓ Continue to success
- **If no valid transaction:** ❌ Return error 402 - "First add payment to this event (minimum ₹500) to proceed"

### Success Response (200 OK):

```json
{
  "success": true,
  "data": {
    "walletId": "wallet-uuid",
    "userId": "user-uuid",
    "userName": "Customer Name",
    "balance": 1500.5,
    "isWalletLocked": false,
    "eventId": "event-uuid"
  }
}
```

### Error Responses:

| Status | Error Message                      | Scenario                                 |
| ------ | ---------------------------------- | ---------------------------------------- |
| 404    | QR not valid                       | Wallet with QR not found                 |
| 404    | User not found - QR not valid      | User ID doesn't exist in database        |
| 423    | Wallet is locked...                | Wallet lock status is true               |
| 402    | First add payment to this event... | No transaction >= ₹500 found             |
| 401    | Authorization token is required    | Missing/invalid JWT token                |
| 403    | Only stall vendors can validate... | User role is not STALL_VENDOR            |
| 400    | Bad Request                        | Invalid QR format, empty QR string, etc. |
| 500    | Internal Server Error              | Unexpected exception                     |

---

## Frontend Flow (React Native - Redemption App)

### 1. QR Scan Screen

```
┌─────────────────────────────┐
│  📱 SCAN QR CODE            │
│                             │
│  [Camera/Scanner UI]        │
│                             │
│  "Scan customer's wallet QR"│
│                             │
│  [Scan & Rescan buttons]    │
└─────────────────────────────┘
```

### 2. QR Validation Process

**When scanned, immediately show:**

- Loading modal with message "Processing QR validation..."
- Animated loader visible
- Cannot dismiss

```
┌─────────────────────────────┐
│                             │
│  ⏳ Processing...           │
│                             │
│  Validating wallet QR code  │
│  Please wait...             │
│                             │
│              [Loader]       │
│                             │
└─────────────────────────────┘
```

### 3. Validation Response Handling

#### Success Path:

- Hide processing modal
- Extract from response:
  - Customer Name
  - Wallet Balance
  - Wallet ID
  - User ID
  - Event ID
  - Lock Status
- Proceed to Confirmation Modal

#### Error Path:

- Hide processing modal
- Display error in styled red modal with:
  - Error message from backend
  - "Rescan" or "Try Again" button
  - Error details for debugging (in console)

### 4. Confirmation Modal (If QR Valid)

```
┌─────────────────────────────┐
│                             │
│  Confirm Checkout           │
│  Are you sure you want to   │
│  proceed with this checkout?│
│                             │
│  ┌───────────────────────┐  │
│  │ 👤 Customer Name      │  │
│  │ [Customer Full Name]  │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │ 💰 Current Balance    │  │
│  │ ₹1,500.50            │  │
│  │ 🔓 Wallet Unlocked   │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │ 🛒 Order Summary      │  │
│  │ Item 1 x 2  ₹500    │  │
│  │ Item 2 x 1  ₹250    │  │
│  │ ─────────────────── │  │
│  │ Total      ₹750    │  │
│  └───────────────────────┘  │
│                             │
│  [Yes, Checkout]  [Cancel]  │
│                             │
└─────────────────────────────┘
```

**Confirmation Modal Features:**

- Shows customer name prominently
- Shows available balance
- Shows wallet lock status
- Shows order breakdown (items and total)
- Cannot dismiss by tapping outside
- "Yes, Checkout" button triggers payment
- "Cancel" button closes modal and clears data

### 5. Payment Processing

```
┌─────────────────────────────┐
│                             │
│  ⏳ Verifying Payment...    │
│                             │
│  Processing checkout       │
│  (processing = true)       │
│                             │
│              [Loader]       │
│                             │
└─────────────────────────────┘
```

### 6. Final Response

#### Success Path:

```
┌─────────────────────────────┐
│                             │
│  ✅ Payment Successful      │
│                             │
│  Transaction ID: TXN-12345  │
│  Total Deducted: ₹750     │
│                             │
│  Items Breakdown:          │
│  Item 1 x 2  ₹500         │
│  Item 2 x 1  ₹250         │
│                             │
│  [Show QR] [View Wallet]    │
│  [Explore Events]           │
│                             │
└─────────────────────────────┘
```

#### Error Path:

```
┌─────────────────────────────┐
│                             │
│  ❌ Payment Failed          │
│                             │
│  ┌─────────────────────────┐│
│  │🔴 Error Message from    ││
│  │   backend service       ││
│  │   (clear description)   ││
│  └─────────────────────────┘│
│                             │
│  [Try Again]  [Modify Order]│
│                             │
└─────────────────────────────┘
```

---

## Error Messages & States

### Frontend Error Display

All errors show in styled red container with:

- Background: `rgba(239,68,68,0.1)` (light red)
- Border: `rgba(239,68,68,0.3)` (red)
- Text Color: `#fca5a5` (light red)
- Font Size: 13px, Weight: 600
- Line Height: 18px
- Padding: 14px

### Common Error Scenarios:

**1. Invalid QR**

- Message: "QR not valid"
- Action: Show "Rescan QR" button

**2. User Not Found**

- Message: "User not found - QR not valid"
- Action: Show "Rescan QR" button
- Reason: User ID from wallet doesn't exist in database

**3. Wallet Locked**

- Message: "Wallet is locked. Unlock the wallet from the app first and then proceed with the payment"
- Action: Prevent checkout, show prominent lock icon
- Solution: User must unlock wallet from wallet/main app

**4. Insufficient Balance**

- Message: "Insufficient balance. Available: ₹X, Required: ₹Y"
- Action: Show "Modify Order" button to reduce cart
- Or: Show "Top Up Wallet" button

**5. First Payment Required**

- Message: "First add payment to this event (minimum ₹500) to proceed"
- Action: User must add initial payment from main app

**6. Network Error**

- Message: "Connection refused. The server may be down or unreachable."
- Action: Show "Try Again" button

**7. Auth Error**

- Message: "Authentication failed. Your token is invalid or expired. Please sign in again."
- Action: Navigate to login screen

---

## Console Logging

### Backend Logs

**QR Validation Starting:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔍 QR VALIDATION - NEW FLOW STARTED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Each Step:**

```
STEP 1️⃣: Searching for wallet with this QR string...
✅ STEP 1 PASSED: Wallet found
   Wallet ID: wallet-123
   User ID (from wallet): user-456
```

**Success:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ ALL VALIDATIONS PASSED - QR IS VALID
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Ready for Checkout:
  Customer: John Doe
  Wallet Balance: ₹1,500.50
```

### Frontend Logs

**QR Scanned:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📱 QR CODE SCANNED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Raw QR Data: user-123::wallet-456::1623456789
```

**QR Validation Success:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ QR VALIDATION SUCCESSFUL
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Wallet Details:
  User Name: John Doe
  Balance: ₹1,500.50
```

**Error Logged:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ QR VALIDATION FAILED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Full Error Details:
  Status: 404
  Message: QR not valid - Wallet not found
```

**Checkout Error on Screen:**

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔴 CHECKOUT ERROR DISPLAYED ON ORDER SCREEN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Error Message: Wallet is locked. Unlock the wallet...
Timestamp: 18/2/2026, 5:14:13 pm
```

---

## Key Implementation Details

### Why QR String Not Parsed Directly?

The old flow tried to extract userId from QR string using `.split("::")`. This caused issues because:

1. QR format might vary
2. Parsing errors lead to "User not found"
3. Wallet is the source of truth, not QR string

**New approach:** Use wallet's stored userId directly

### Wallet as Source of Truth

- QR string is validated by comparing with wallet's stored QR string
- User ID is extracted from wallet, not from QR parsing
- This ensures data consistency

### Error Message Extraction

Frontend checks multiple response formats:

1. Backend ApiResponse: `data.error` or `data.message`
2. HTTP Status codes: 404, 423, 402, 401, etc.
3. Fallback messages based on status

### Loading States

- QR Validation: "Processing QR validation..."
- Payment Processing: "Processing payment..."
- Payment Verification: "Verifying payment..."

### Clear User Experience

1. **Scan** → See loading
2. **Validate** → See confirmation modal OR error
3. **Confirm** → See payment processing
4. **Result** → See success OR specific error with action buttons

---

## Code Files Modified

### Backend

- **WalletController.java** - `/api/wallet/validate-qr` endpoint (NEW FLOW)

### Frontend

- **use-validate-qr.ts** - QR validation with new error extraction
- **order.tsx** - Confirmation modal and error handling
- **use-checkout.ts** - Improved error message extraction

---

## Testing Checklist

- [ ] Valid QR → Show confirmation modal
- [ ] Invalid QR → Show "QR not valid" error
- [ ] Non-existent user → Show "User not found - QR not valid"
- [ ] Locked wallet → Show "Wallet is locked..." error
- [ ] Insufficient balance → Show "Insufficient balance" error
- [ ] Successful checkout → Show success with transaction details
- [ ] Network error → Show "Connection refused" message
- [ ] Expired token → Show "Please sign in again" message
- [ ] All loading states visible → Processing, Verifying
- [ ] All console logs present → Debugging information clear
- [ ] Error messages clear and actionable → User knows what to do

---

## Summary

This flow ensures:
✅ Clear validation steps  
✅ Specific error messages  
✅ User confirmation before checkout  
✅ Proper loading indicators  
✅ Mobile-friendly UX  
✅ Comprehensive logging for debugging  
✅ Error recovery options  
✅ Source of truth (wallet, not QR parsing)
