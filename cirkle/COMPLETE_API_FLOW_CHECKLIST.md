# Complete API Flow Checklist ✅

## Mobile App Wallet Topup Flow

### Request Path

```
voulnteer-mobile-app/recharge-wallet.tsx
  → volunteerApi.topupWalletCash(topupUserId, customerId, eventId, amount)
  → POST /api/volunteer/wallet/topup
  → VolunteerService.topupWallet()
  → Returns: { success: true, transactionId: "...", message: "..." }
```

### Data Written

```
wallet_transactions (WalletWalletTransaction)
├─ userId: customerId (customer who received money) ✅
├─ eventId: eventId ✅
├─ amount: topup amount ✅
├─ transaction_type: CREDIT ✅
├─ topup_user_id: topupUserId (topup operator) ✅
├─ final_balance: calculated balance ✅
├─ payment_method: CASH/UPI/RAZORPAY ✅
└─ description: "Event top-up via CASH" ✅

topup_balances
├─ userId: customerId ✅
├─ eventId: eventId ✅
└─ availableBalance += amount ✅

wallets (Wallet)
├─ userId: customerId ✅
├─ eventId: eventId ✅
└─ balance += amount ✅
```

### Response Validation

- Mobile app checks: `response.success === true` ✅
- On success: Calls `invalidateWalletBalance()` to refresh UI ✅
- On failure: Shows `response.message` error ✅

---

## QR Validation Flow

### Request Path

```
redemption-app/use-validate-qr.ts
  → POST /api/wallet/validate-qr { qrString, eventId }
  → WalletController.validateByQrString()
  → Returns: { success: true, data: { walletId, userId, userName, balance, isWalletLocked, holdId } }
```

### Balance Source ✅

- Reads from: `Wallet.balance` (updated by topup operations)
- This field is updated during topup, so it reflects latest balance
- No stale data issues ✅

### Data Written

```
validation_holds (ValidationHold)
├─ walletId: from Wallet ✅
├─ userId: from Wallet ✅
├─ eventId: from request ✅
├─ qrString: from request ✅
├─ status: ACTIVE ✅
├─ expiresAt: now + 20 minutes ✅
└─ holdId: generated UUID ✅
```

### Response Structure ✅

```typescript
{
  walletId: string,
  userId: string,
  userName: string,
  balance: number,           // From Wallet.balance
  isWalletLocked: boolean,
  eventId: string,
  holdId: string            // For checkout reference
}
```

---

## Redemption Checkout Flow

### Request Path

```
redemption-app/use-checkout.ts
  → POST /checkout {
      eventId, userId, walletId, items, totalAmount, qrString, holdId
    }
  → RedemptionController.checkout()
  → RedemptionService.processCheckout()
  → Returns: { success: true, transactionId: "...", ... }
```

### Validation Steps ✅

1. Check `holdId` exists ✅
2. Check `ValidationHold` status = ACTIVE ✅
3. Check `ValidationHold` not expired ✅
4. Check `walletId` matches hold ✅
5. Check `userId` matches hold ✅

### Deduction Order ✅

1. Deduct from TopupBalance (event-specific) first
2. Deduct overflow from TopupBalance (GENERAL)
3. Deduct from Wallet.balance (event-specific) first
4. Deduct overflow from Wallet.balance (GENERAL)

### Data Written

```
wallet_transactions (WalletWalletTransaction) - TWO records
├─ Record 1: Event Wallet Debit
│  ├─ userId: customerId (customer who paid) ✅
│  ├─ eventId: eventId ✅
│  ├─ amount: deductedFromEventWallet ✅
│  ├─ transaction_type: DEBIT ✅
│  ├─ stall_user_id: vendorId (stall vendor who made sale) ✅
│  ├─ final_balance: calculated balance ✅
│  ├─ payment_method: WALLET_STALL ✅
│  └─ description: "Stall purchase at..." ✅
│
└─ Record 2: General Wallet Debit (if overflow)
   ├─ userId: customerId ✅
   ├─ eventId: GENERAL ✅
   ├─ amount: deductedFromGeneralWallet ✅
   ├─ transaction_type: DEBIT ✅
   ├─ stall_user_id: vendorId ✅
   ├─ final_balance: calculated balance ✅
   ├─ payment_method: WALLET_STALL ✅
   └─ description: "Stall purchase overflow..." ✅

topup_balances
├─ availableBalance -= amount ✅
└─ usedBalance += amount ✅

wallets (Wallet)
├─ balance -= amount ✅
└─ updated_at: now ✅

validation_holds (ValidationHold)
└─ status: COMPLETED ✅

redemption_transactions (RedemptionTransaction)
├─ userId: customerId ✅
├─ eventId: eventId ✅
├─ totalAmount: totalAmount ✅
└─ status: COMPLETED ✅
```

### Response Structure ✅

```typescript
{
  success: boolean,
  transactionId: string,
  totalAmount: number,
  items: CheckoutItem[],
  status: "COMPLETED" | "FAILED"
}
```

---

## Balance Calculation Verification

### Latest Balance Query ✅

```typescript
// Get latest balance from wallet_transactions
SELECT * FROM wallet_transactions
WHERE user_id = ? AND event_id = ?
ORDER BY transaction_date DESC
LIMIT 1
// Returns: final_balance field is THE BALANCE
```

### Balance Flow ✅

```
Initial: 0
After Topup ₹500: final_balance = 500
After Topup ₹200: final_balance = 700
After Purchase ₹300: final_balance = 400
After Purchase ₹100: final_balance = 300
```

Each transaction stores its own `final_balance` which is calculated as:

- CREDIT: currentBalance + amount
- DEBIT: currentBalance - amount

---

## Error Handling

### Mobile App ✅

- Success: `response.success === true`
- Failure: Shows `response.message` or `error.response.data.message`
- Network Error: Caught try-catch, shows generic error

### Redemption App ✅

- QR Valid: Returns data with `valid: true`
- QR Invalid: Returns `{ valid: false, message: "..." }`
- Checkout Success: `response.success === true`
- Checkout Error: Shows specific error message

### Backend ✅

- 200 OK: Transaction successful
- 400 Bad Request: Missing required fields
- 401 Unauthorized: Invalid/expired token
- 402 Payment Required: Missing minimum transaction
- 403 Forbidden: User role mismatch
- 404 Not Found: Wallet/User not found
- 423 Locked: Wallet is locked
- 500 Internal Server Error: Database/transaction error

---

## Testing Checklist

### Unit Test: Topup

- [ ] CASH topup creates WalletWalletTransaction with topup_user_id
- [ ] UPI topup creates WalletWalletTransaction with topup_user_id
- [ ] finalBalance is calculated correctly
- [ ] Wallet.balance is updated
- [ ] TopupBalance is updated
- [ ] Response returns success=true with transactionId

### Unit Test: QR Validation

- [ ] Valid QR returns balance from Wallet.balance field
- [ ] Balance reflects recent topup
- [ ] ValidationHold created with 20-min expiry
- [ ] Locked wallet returns appropriate error
- [ ] Missing ₹500+ transaction returns appropriate error

### Unit Test: Redemption

- [ ] Checkout requires valid holdId
- [ ] Checkout creates DEBIT WalletWalletTransaction with stall_user_id
- [ ] finalBalance calculated and stored
- [ ] Both event and general wallets updated
- [ ] TopupBalance deducted correctly
- [ ] ValidationHold marked COMPLETED

### Integration Test: Full Flow

- [ ] Topup ₹500 via mobile app
- [ ] QR validation returns ₹500 balance
- [ ] Redemption ₹300 via stall app
- [ ] Balance query returns ₹200
- [ ] Next topup uses ₹200 as starting balance

---

## Endpoints Summary

| Endpoint                                       | Method | Auth   | Module     | Status |
| ---------------------------------------------- | ------ | ------ | ---------- | ------ |
| `/api/volunteer/wallet/topup`                  | POST   | Bearer | Volunteer  | ✅     |
| `/api/volunteer/wallet/initiate-upi`           | POST   | Bearer | Volunteer  | ✅     |
| `/api/volunteer/wallet/verify-upi`             | POST   | Bearer | Volunteer  | ✅     |
| `/api/volunteer/wallet/confirm-manual-payment` | POST   | Bearer | Volunteer  | ✅     |
| `/api/wallet/validate-qr`                      | POST   | Bearer | Wallet     | ✅     |
| `/api/wallet/validate-qr/cancel`               | POST   | Bearer | Wallet     | ✅     |
| `/checkout`                                    | POST   | Bearer | Redemption | ✅     |
| `/api/wallet/overview`                         | GET    | Bearer | Wallet     | ✅     |
| `/api/wallet/general/balance`                  | GET    | Bearer | Wallet     | ✅     |
| `/api/wallet/event/{eventId}`                  | GET    | Bearer | Wallet     | ✅     |

---

## Code Review Items

### VolunteerService.java

- ✅ topupWallet() creates WalletWalletTransaction only
- ✅ Sets topup_user_id field
- ✅ Calculates and sets finalBalance
- ✅ Updates Wallet.balance
- ✅ Updates TopupBalance
- ✅ Returns transactionId from WalletWalletTransaction

### RedemptionService.java

- ✅ Creates WalletWalletTransaction with stall_user_id
- ✅ Sets transactionType = DEBIT
- ✅ Calculates and sets finalBalance
- ✅ Updates Wallet.balance
- ✅ Updates TopupBalance
- ✅ Marks ValidationHold as COMPLETED

### WalletController.java

- ✅ /validate-qr reads balance from Wallet.balance
- ✅ Returns balance field in response
- ✅ Creates ValidationHold with proper expiry

---

## Deployment Checklist

- ✅ All endpoints implemented in backend
- ✅ All API calls correctly structured in frontend apps
- ✅ Single source of truth (wallet_transactions)
- ✅ Balance synchronization (Wallet table + wallet_transactions)
- ✅ Transaction tracking (topup_user_id + stall_user_id)
- ✅ Error handling & validation
- ✅ Response structures match expectations
- ✅ Database schema complete
- ✅ No compilation errors
- ✅ Ready for testing & deployment
