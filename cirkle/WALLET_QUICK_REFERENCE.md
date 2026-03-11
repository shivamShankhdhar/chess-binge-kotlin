# Wallet Data Flow - Quick Reference Guide

## 📊 All Related Tables & Their Purpose

```
┌─────────────────────────────────────────────────────────────┐
│                    WALLET_TRANSACTIONS                      │ ← PRIMARY TABLE
├─────────────────────────────────────────────────────────────┤
│ • Stores ALL transactions (CREDIT & DEBIT)                  │
│ • Source of truth for balance calculation                   │
│ • Fields: transactionId, userId, eventId, amount,           │
│           transactionType ("CREDIT"/"DEBIT"), date,         │
│           description, paymentMethod                        │
└─────────────────────────────────────────────────────────────┘
            ▲                    ▼
    Queries from          Updates from
    - WalletService       - TopupFlow
    - Controllers         - RedemptionFlow
            │                    │
            └────────┬───────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
        ▼            ▼            ▼
    ┌────────┐  ┌────────┐  ┌──────────────┐
    │ wallet │  │ event  │  │ topup_       │
    │        │  │        │  │ balances     │
    │ (Meta) │  │(Enrich)│  │ (Tracking)   │
    └────────┘  └────────┘  └──────────────┘
        │
        └─→ validation_holds (Lock during checkout)

    ┌─────────────────────────────────────────┐
    │     redemption_transactions &           │
    │     transaction_items (Details)         │
    └─────────────────────────────────────────┘
```

---

## 💾 Database Tables Summary

| Table                       | Purpose              | Updated By        | Read By               |
| --------------------------- | -------------------- | ----------------- | --------------------- |
| **wallet_transactions**     | Transaction history  | Topup, Redemption | Balance calc, Display |
| **wallet**                  | Metadata, QR string  | First topup       | Overview, Validation  |
| **topup_balances**          | Redemption tracking  | Topup, Checkout   | Checkout validation   |
| **validation_holds**        | Prevent double-spend | QR validation     | Checkout release      |
| **redemption_transactions** | Checkout records     | RedemptionService | Auditing              |
| **transaction_items**       | Line items           | RedemptionService | Auditing              |
| **event**                   | Event info           | Event module      | Enrichment            |
| **user**                    | User verification    | Auth module       | QR validation         |

---

## 🔄 Key Data Flows

### Flow 1: User Adds Topup

```
User → WalletController.verifyPayment()
    ├─ Create WalletWalletTransaction
    │  └─ type: "CREDIT" ✅
    ├─ Update TopupBalance (available += amount)
    ├─ Update Wallet (balance += amount)
    └─ Generate QR string (if first topup)

Result: wallet_transactions shows +1000 CREDIT
Balance: 1000 ✓
```

### Flow 2: Vendor Scans QR

```
Vendor QR Scan → WalletController.validateByQrString()
    ├─ Find wallet by QR string
    ├─ Verify user exists
    ├─ Check ₹500+ transaction for event
    ├─ Create ValidationHold (20 min timeout)
    └─ Return wallet details

Result: Checkout can proceed
```

### Flow 3: Vendor Confirms Checkout

```
Vendor Checkout → RedemptionService.processCheckout()
    ├─ Verify balance sufficiency
    ├─ Create RedemptionTransaction
    ├─ Create TransactionItems (line items)
    ├─ Create DEBIT transactions (1-2)
    │  ├─ Event wallet DEBIT
    │  └─ General wallet DEBIT (if overflow)
    ├─ Update TopupBalance
    └─ Release ValidationHold

Result: wallet_transactions shows -500 DEBIT
Balance: 1000 - 500 = 500 ✓
```

### Flow 4: Frontend Gets Overview

```
Frontend → GET /api/wallet/overview
WalletController → WalletService.getUserWalletOverview()
    ├─ Query wallet_transactions (all)
    ├─ Calculate totalBalance
    ├─ Calculate generalWallet
    ├─ Calculate eventBalances
    ├─ Get wallet metadata (QR string, lock status)
    └─ Enrich events with names from event table

Result: {
  walletBalance: 500,
  generalWalletAmount: 500,
  qrString: "...",
  eventWallets: [...]
}
```

### Flow 5: Frontend Gets Transactions

```
Frontend → GET /api/wallet/transactions
WalletController.getAllTransactions()
    ├─ Query wallet_transactions (all, sorted by date)
    ├─ For each transaction:
    │  ├─ Map fields (type uppercase)
    │  └─ Enrich with event name from event table
    └─ Return with eventName and eventImage

Result: [{
  type: "DEBIT",
  amount: 500,
  eventName: "Tech Conference",
  paymentMethod: "WALLET_STALL"
}, ...]
```

---

## ✅ Data Validation

### Transaction Type Values

```
In Database: ALWAYS UPPERCASE
  ✅ "CREDIT" - topup, payment in
  ✅ "DEBIT" - redemption, purchase out

Code Comparisons: ALWAYS CASE-INSENSITIVE
  ✅ equalsIgnoreCase("credit")
  ✅ equalsIgnoreCase("debit")

Frontend Display: ALWAYS UPPERCASE
  ✅ Converts to uppercase: (tx.type || '').toUpperCase()
  ✅ Shows "CREDIT" or "DEBIT" only
```

### Balance Calculation Formula

```
walletBalance = Σ(CREDIT amounts) - Σ(DEBIT amounts)

For Event Wallets:
eventBalance = Σ(CREDIT where eventId=EVENT) - Σ(DEBIT where eventId=EVENT)

Non-Negative Enforcement:
finalBalance = max(0, calculated_balance)
```

### Event Enrichment

```
Backend Enrichment (at API layer):
  wallet_transactions.eventId
    └─→ Query event table
         └─→ Get title → eventName
         └─→ Get images → eventImage

No frontend queries needed - all enriched by backend
```

---

## 🔌 API Endpoints

### GET /api/wallet/overview

```
Purpose: Get wallet balance and metadata
Auth: JWT Bearer token required
Response: {
  "walletBalance": 500.0,
  "generalWalletAmount": 500.0,
  "qrString": "...",
  "isWalletLocked": false,
  "eventWallets": [{
    "eventId": "EVENT_123",
    "eventName": "Tech Conf",
    "balance": 0.0
  }]
}
```

### GET /api/wallet/transactions

```
Purpose: Get transaction history with enrichment
Auth: JWT Bearer token required
Params: ?eventId=EVENT_123 (optional filter)
Response: {
  "transactions": [{
    "transactionId": "...",
    "date": "2024-03-01T16:45:00+05:30",
    "type": "DEBIT",
    "amount": 500.0,
    "description": "Stall purchase...",
    "paymentMethod": "WALLET_STALL",
    "eventId": "EVENT_123",
    "eventName": "Tech Conference",  ← ENRICHED
    "eventImage": "https://..."      ← ENRICHED
  }],
  "count": 1
}
```

### POST /api/wallet/validate-qr

```
Purpose: Validate QR before checkout (STALL_VENDOR only)
Auth: JWT Bearer token (STALL_VENDOR role required)
Body: {
  "qrString": "user-UUID-timestamp",
  "eventId": "EVENT_123"
}
Response: {
  "success": true,
  "walletId": "...",
  "userId": "...",
  "balance": 500.0,
  "isWalletLocked": false
}
```

---

## 🐛 Issues Found & Fixed

| Issue                               | Status   | Files                                        |
| ----------------------------------- | -------- | -------------------------------------------- |
| "credit" vs "CREDIT" case mismatch  | ✅ FIXED | WalletController (2x), VolunteerService (3x) |
| Unsafe .equals() comparisons        | ✅ FIXED | WalletController getEventTransactions        |
| eventName not returned from API     | ✅ FIXED | getAllTransactions endpoint                  |
| eventId not in transaction response | ✅ FIXED | Transaction mapping                          |

---

## 📱 Frontend Display Components

### Balance Card

```
Shows: totalBalance (from wallet_transactions sum)
Calculation: CREDIT - DEBIT (from WalletService)
Range: ₹0 to ∞
Format: ₹{amount} with comma separator + words
```

### Wallet Breakdowns

```
General Wallet: Amount from "GENERAL" eventId transactions
Event Wallets: Amount from event-specific eventId transactions
Each shows: eventName (from event table), balance, image
```

### Transaction Item

```
Icon: ↓ green if CREDIT, ↑ red if DEBIT
Description: From transaction description field
Date: Formatted from transactionDate (IST timezone)
Event Badge: Shows eventName (red) if DEBIT transaction
Amount: Shows with ± sign, color-coded
```

---

## 🚀 Production Readiness

✅ **Data Integrity**

- Consistent transaction types (UPPERCASE)
- Non-negative balance enforcement
- Case-insensitive comparisons

✅ **Enrichment**

- Event names from event table
- Event images attached
- Clear "General Wallet" label

✅ **Security**

- JWT validation on all endpoints
- Role-based access (STALL_VENDOR for QR validation)
- ValidationHold prevents double-spending

✅ **Error Handling**

- Proper HTTP status codes
- Meaningful error messages
- Transaction atomicity (all-or-nothing)

✅ **Performance**

- Indexed queries (user_id, event_id, transaction_date)
- Minimal enrichment queries
- Efficient balance calculations

---

## 📖 Related Documentation

See these comprehensive documents for details:

- `COMPLETE_WALLET_DATA_FLOW.md` - Full architectural overview
- `WALLET_CASE_SENSITIVITY_FIX_COMPLETE.md` - Issue details and fixes
- `WALLET_FINAL_VERIFICATION_REPORT.md` - Complete verification report

---

## Quick Debug Commands

### Check transaction type consistency

```sql
SELECT DISTINCT transaction_type FROM wallet_transactions
ORDER BY transaction_type;
-- Should show: CREDIT, DEBIT (uppercase only)
```

### Check balance calculation

```sql
SELECT
  user_id,
  event_id,
  SUM(CASE WHEN transaction_type = 'CREDIT' THEN amount ELSE 0 END) as credits,
  SUM(CASE WHEN transaction_type = 'DEBIT' THEN amount ELSE 0 END) as debits,
  (SUM(CASE WHEN transaction_type = 'CREDIT' THEN amount ELSE 0 END) -
   SUM(CASE WHEN transaction_type = 'DEBIT' THEN amount ELSE 0 END)) as balance
FROM wallet_transactions
GROUP BY user_id, event_id;
```

### Check for orphaned events

```sql
SELECT w.event_id, w.user_id, COUNT(*) as tx_count
FROM wallet_transactions w
LEFT JOIN event e ON w.event_id = e.id
WHERE w.event_id != 'GENERAL' AND e.id IS NULL
GROUP BY w.event_id, w.user_id;
-- Should return: no rows (all events should exist)
```

---

**Last Updated:** March 1, 2026
**Verified By:** Code Audit & Compilation Check
**Status:** ✅ Production Ready
