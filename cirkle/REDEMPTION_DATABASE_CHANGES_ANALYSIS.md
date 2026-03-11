# Redemption App - Database Changes & Flow Analysis

## Overview

This document details the complete redemption checkout flow, all databases affected, endpoints used, and the exact sequence of database modifications.

---

## Redemption Checkout Flow

### Main Flow: QR Validation → Confirmation → Checkout

```
┌─────────────────────────────────────────┐
│ 1. STALL VENDOR SCANS CUSTOMER QR      │
│    (Redemption App)                    │
└─────────────────┬───────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────┐
│ 2. POST /api/wallet/validate-qr        │
│    Security: JWT Token + STALL_VENDOR  │
│                                        │
│    Request:                            │
│    {                                   │
│      qrString: "userId::walletId::ts", │
│      eventId: "event-uuid"             │
│    }                                   │
└─────────────────┬───────────────────────┘
                  │
                  ↓
        VALIDATIONS PERFORMED
                  │
         ┌────────┴────────┐
         │                 │
    ✅ VALID          ❌ INVALID
         │                 │
         ↓                 ↓
    Confirmation      Error Response
       Modal              (404/423/402)
         │                 │
         ↓                 ↓
    [Confirm]      [Rescan/Cancel]
         │
         ↓
┌─────────────────────────────────────────┐
│ 3. POST /api/checkout                  │
│    Process Payment                     │
│                                        │
│    Request:                            │
│    {                                   │
│      eventId: "event-uuid",            │
│      userId: "user-uuid",              │
│      qrString: "...",                  │
│      totalAmount: 750.00,              │
│      items: [                          │
│        {itemId: "item1", qty: 2},     │
│        {itemId: "item2", qty: 1}      │
│      ]                                 │
│    }                                   │
└─────────────────┬───────────────────────┘
                  │
                  ↓
        DATABASE TRANSACTIONS BEGIN
                  │
    ┌────────────┴────────────┐
    │                         │
VALIDATE STEP 1          DEDUCT STEP 1
Wallet Balance           Topup Balance
    │                         │
    ↓                         ↓
Check sufficient        Reduce event-specific
balance                 topup balance
    │                         │
    ├──────────┬──────────────┤
    │          │              │
    ↓          ↓              ↓
 STEP 2A    STEP 2B        STEP 3
Create      Deduct from  Create Wallet
Redemption  Wallet Entity Transaction
Transaction Balance       Records
    │          │              │
    ↓          ↓              ↓
INSERT INTO  UPDATE       INSERT INTO
redemption-  wallet_*     wallet_
transactions from event   transactions
             to GENERAL
             (split)

```

---

## Databases & Tables Modified During Checkout

### Total: **6 Tables Modified** across multiple databases

#### 1. **Stall Service Database**

- **redemption_transactions** ✅ INSERT
- **transaction_items** ✅ INSERT

#### 2. **Wallet Service Database**

- **topup_balances** ✅ UPDATE
- **wallet\_\* (actual wallet entities)** ✅ UPDATE
- **wallet_transactions** ✅ INSERT (2 records - event + general)

#### 3. **Validation Database**

- **validation_holds** ✅ CREATE/UPDATE (during QR validation)

---

## Detailed Database Changes During Checkout

### Phase 1: QR Validation (`POST /api/wallet/validate-qr`)

**Tables Read:**

```
- Users table → Find user by ID
- Wallets table → Find wallet by QR string
- Wallet Transactions table → Check transaction history (₹500+ requirement)
- Validation Holds table → Check if QR already in use
- Events table → Enrich response with event name
```

**Tables Modified:**

```
- Validation Holds table (INSERT new hold record)
  Status: ACTIVE
  Expires: 20 minutes from now
  Purpose: Prevent concurrent checkouts with same QR
```

**Response:** Returns wallet balance, user details, event info

---

### Phase 2: Checkout Processing (`POST /api/checkout`)

#### STEP 1: Create Redemption Record

**Table: `redemption_transactions`**

```sql
INSERT INTO redemption_transactions
  (transaction_id, user_id, event_id, total_amount, status, created_at)
VALUES
  (UUID(), 'user-123', 'event-456', 750.00, 'COMPLETED', NOW())
```

**Fields Set:**

- `transaction_id`: UUID
- `user_id`: From QR validation
- `event_id`: From request
- `total_amount`: Cart total
- `status`: COMPLETED
- `created_at`: Current timestamp

---

#### STEP 2: Create Transaction Items

**Table: `transaction_items`**

```sql
INSERT INTO transaction_items
  (transaction_id, menu_item_id, item_name, quantity, unit_price, total_price)
VALUES
  ('txn-uuid', 'item-1', 'Pizza', 2, 250.00, 500.00),
  ('txn-uuid', 'item-2', 'Coke', 1, 250.00, 250.00)
```

**Fields Set (for each item):**

- `transaction_id`: Links to redemption transaction
- `menu_item_id`: From cart
- `item_name`: From menu
- `quantity`: From cart
- `unit_price`: From menu
- `total_price`: quantity × unit_price

---

#### STEP 3: Deduct from Topup Balance

**Table: `topup_balances`**

**Deduction Strategy:**

1. First, deduct from event-specific topup balance (if available)
2. Then, deduct overflow from GENERAL topup balance

```sql
-- Event-specific topup deduction
UPDATE topup_balances
SET used_balance = used_balance + 500.00,
    available_balance = available_balance - 500.00,
    updated_at = NOW()
WHERE user_id = 'user-123' AND event_id = 'event-456'

-- General topup deduction (if event balance insufficient)
UPDATE topup_balances
SET used_balance = used_balance + 250.00,
    available_balance = available_balance - 250.00,
    updated_at = NOW()
WHERE user_id = 'user-123' AND event_id = 'GENERAL'
```

**Fields Modified:**

- `used_balance`: ✅ Increased
- `available_balance`: ✅ Decreased
- `updated_at`: ✅ Current timestamp

---

#### STEP 4: Deduct from Wallet Balance

**Table: `wallet_*` (Actual wallet entities by event/user)**

**Deduction Strategy (Same as topup):**

1. First from event wallet
2. Then from GENERAL wallet

```sql
-- Event wallet balance deduction
UPDATE wallet
SET balance = balance - 500.00,
    updated_at = NOW()
WHERE user_id = 'user-123' AND event_id = 'event-456'

-- General wallet balance deduction
UPDATE wallet
SET balance = balance - 250.00,
    updated_at = NOW()
WHERE user_id = 'user-123' AND event_id = 'GENERAL'
```

**Fields Modified:**

- `balance`: ✅ Decreased (ensures cirkle-app shows correct balance)
- `updated_at`: ✅ Current timestamp

---

#### STEP 5: Create Wallet Transaction Records

**Table: `wallet_transactions`** (2 inserts)

**Record 1: Event Wallet Debit**

```sql
INSERT INTO wallet_transactions
  (transaction_id, user_id, event_id, amount, transaction_type,
   transaction_date, description, payment_method)
VALUES
  (UUID(), 'user-123', 'event-456', 500.00, 'DEBIT',
   NOW(), 'Stall purchase at Event Name: Pizza x2...', 'WALLET_STALL')
```

**Record 2: General Wallet Debit (if overflow)**

```sql
INSERT INTO wallet_transactions
  (transaction_id, user_id, event_id, amount, transaction_type,
   transaction_date, description, payment_method)
VALUES
  (UUID(), 'user-123', 'GENERAL', 250.00, 'DEBIT',
   NOW(), 'Stall purchase overflow from Event Name: Pizza x2...', 'WALLET_STALL')
```

**Fields Set:**

- `transaction_id`: UUID (auto-generated)
- `user_id`: From QR validation
- `event_id`: 'event-456' for event wallet, 'GENERAL' for general
- `amount`: Amount deducted from that specific wallet
- `transaction_type`: **'DEBIT'** (for debits/redemptions)
- `transaction_date`: Current timestamp (IST)
- `description`: "Stall purchase at {eventName}: {items}"
- `payment_method`: **'WALLET_STALL'** (identifies redemption source)

**Note:** These records appear in cirkle-app wallet transactions feed with:

- Type: DEBIT (shown as red ↑ arrow)
- Amount: ₹{amount}
- Description: Shows items purchased
- Event Badge: Shows event name for easy identification

---

## Complete Database State After Checkout

```
┌────────────────────────────────────────────────────────────┐
│         BEFORE CHECKOUT        │      AFTER CHECKOUT      │
├────────────────────────────────┼──────────────────────────┤
│ User wallet balance: ₹2000      │ User wallet balance: ₹1250
│ Event topup available: ₹1000    │ Event topup available: ₹500
│ General topup available: ₹1000  │ General topup available: ₹750
│ Redemption transactions: 5      │ Redemption transactions: 6
│ Transaction items: 12           │ Transaction items: 14
│ Wallet transactions: 50         │ Wallet transactions: 52
└────────────────────────────────┴──────────────────────────┘
```

---

## Endpoints Summary

| Method | Endpoint                  | Purpose                     | Auth | Role         |
| ------ | ------------------------- | --------------------------- | ---- | ------------ |
| POST   | `/api/wallet/validate-qr` | Validate QR & check balance | JWT  | STALL_VENDOR |
| POST   | `/api/checkout`           | Process stall checkout      | JWT  | STALL_VENDOR |
| GET    | `/wallet/transactions`    | View redemption history     | JWT  | USER         |
| GET    | `/wallet/overview`        | Get wallet balance          | JWT  | USER         |

---

## Response Status & Error Codes

### QR Validation Errors:

- `200 OK` - Valid QR, proceed to checkout
- `404 Not Found` - QR invalid, wallet not found
- `404 Not Found` - User not found
- `423 Locked` - Wallet is locked
- `402 Payment Required` - No ₹500+ transaction for event
- `401 Unauthorized` - Invalid token
- `403 Forbidden` - Not STALL_VENDOR

### Checkout Errors:

- `200 OK` - Checkout successful
- `400 Bad Request` - Validation failed
- `402 Payment Required` - Insufficient balance
- `423 Locked` - Wallet locked
- `500 Server Error` - Transaction creation failed

---

## Key Technical Details

### Transaction Atomicity

- All database operations happen in transaction
- If any step fails, entire checkout fails
- No partial deductions

### Balance Calculation

- Frontend shows **Total Balance = General Wallet + All Event Wallets**
- Backend calculates from transaction history
- Both CREDIT and DEBIT transactions considered with signs

### Wallet Transaction Type Handling

- **'CREDIT'** vs **'DEBIT'** are case-sensitive
- Frontend converts to uppercase: `(tx.type || '').toUpperCase()`
- Must match in balance calculations

### Payment Method Tracking

- Redemptions marked with `'WALLET_STALL'`
- Helps track transaction source
- Shows in transaction history

### Validation Hold

- Created during QR validation
- Expires after 20 minutes
- Prevents concurrent checkouts with same QR
- Status: ACTIVE → Expires → Auto-cleanup

---

## Database Consistency Checks

### After Successful Checkout:

```sql
-- Verify redemption record created
SELECT * FROM redemption_transactions
WHERE user_id = 'user-123' AND event_id = 'event-456'
ORDER BY created_at DESC LIMIT 1

-- Verify transaction items created
SELECT * FROM transaction_items
WHERE transaction_id = '{redemption_id}'

-- Verify topup balance reduced
SELECT available_balance, used_balance FROM topup_balances
WHERE user_id = 'user-123' AND event_id = 'event-456'

-- Verify wallet balance reduced
SELECT balance FROM wallet
WHERE user_id = 'user-123' AND event_id IN ('event-456', 'GENERAL')

-- Verify wallet transactions created
SELECT * FROM wallet_transactions
WHERE user_id = 'user-123' AND payment_method = 'WALLET_STALL'
ORDER BY transaction_date DESC LIMIT 2
```

---

## Summary

✅ **6 Tables Modified:**

1. `redemption_transactions` - INSERT (main transaction record)
2. `transaction_items` - INSERT (line items)
3. `topup_balances` - UPDATE (reduce available balance)
4. `wallet_*` - UPDATE (reduce wallet balance)
5. `wallet_transactions` - INSERT (2 records for balance tracking)
6. `validation_holds` - INSERT (during QR validation)

✅ **Key Endpoints:**

- `POST /api/wallet/validate-qr` - Validate & create hold
- `POST /api/checkout` - Process payment & update all tables

✅ **Transaction Flow:**

- QR Validation (creates hold) → Confirmation → Checkout (atomic transaction)

✅ **Balance Updated:**

- Wallet balance decreases immediately
- Topup balance marked as used
- Wallet transactions recorded for history
- All changes visible in cirkle-app
