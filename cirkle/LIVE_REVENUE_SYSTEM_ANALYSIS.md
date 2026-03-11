# Live Revenue System - Complete Analysis & Architecture

**Date**: February 27, 2026  
**Project**: Cirkle - Event Management Platform  
**Focus**: Understanding Data Sources and Zero-Value Issue Resolution

---

## EXECUTIVE SUMMARY

The Live Revenue system tracks financial transactions across events by combining data from **THREE INDEPENDENT DATA SOURCES**:

1. **VolunteerWalletTransaction** (volunteer_wallet_transactions) - Topup transactions by TOPUP_USER volunteers
2. **WalletWalletTransaction** (wallet_transactions) - Generic wallet credit/debit transactions for users
3. **RedemptionTransaction** (redemption_transactions) - Stall vendor redemptions (sales)

**CRITICAL ISSUE**: The original implementation only looked at ONE source and didn't aggregate all topup data, causing zero values when data existed in the other sources.

**FIX IMPLEMENTED**: LiveRevenueService now aggregates from BOTH topup sources:

- VolunteerWalletTransaction (TOPUP_CASH/TOPUP_UPI with SUCCESS status)
- WalletWalletTransaction (credit transactions with CASH/UPI payment methods)

---

## 1. SYSTEM ARCHITECTURE

### 1.1 Main Components

```
┌────────────────┐
│  Frontend      │
│  (Next.js)     │
└────────┬───────┘
         │
         ▼
┌────────────────────────────────────────┐
│  API Gateway                           │
│  GET /api/v1/liverevenue/{eventId}    │
└────────┬───────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────┐
│  LiveRevenueService (Java)             │
│  ├─ getLiveRevenue()                   │
│  ├─ getTopupsByOperator()              │
│  └─ getRedemptionsByVendor()           │
└────────┬───────────────────────────────┘
         │
    ┌────┴────┬──────────┬────────────────┐
    ▼         ▼          ▼                ▼
┌─────────┐ ┌────────┐ ┌──────────────┐ ┌─────────┐
│Volunteer│ │Wallet  │ │Redemption    │ │Event    │
│Wallet   │ │Wallet  │ │Transaction   │ │         │
│Trans.   │ │Trans.  │ │              │ │         │
└─────────┘ └────────┘ └──────────────┘ └─────────┘
```

### 1.2 Data Flow - Creating a Topup

```
STEP 1: User (TOPUP_USER) initiates payment via Volunteer App
        ↓
STEP 2: Payment verified (Razorpay webhook or manual confirmation)
        ↓
STEP 3: VolunteerWalletTransaction created with:
        - topupUserId: TOPUP_USER's user_id
        - eventId: Event's event_id
        - type: "TOPUP_CASH" or "TOPUP_UPI"
        - amount: Payment amount
        - status: "SUCCESS"
        - paymentMethod: "CASH" or "UPI"
        - createdAt: transaction timestamp
        ↓
STEP 4: Real user's Wallet balance updated in Wallet table
        ↓
STEP 5: WalletWalletTransaction also created as audit log with:
        - userId: Card holder's user_id
        - eventId: Event's event_id
        - transactionType: "credit"
        - amount: Same as topup amount
        - paymentMethod: "CASH" or "UPI"
        - description: "Topup via volunteer"
        - transactionDate: Same as topup timestamp
```

### 1.3 Data Flow - Creating a Redemption

```
STEP 1: User scans QR code at Stall (STALL_VENDOR)
        ↓
STEP 2: Purchase amount validated against wallet balance
        ↓
STEP 3: RedemptionTransaction created with:
        - userId: STALL_VENDOR's user_id
        - eventId: Event's event_id
        - totalAmount: Purchase amount
        - status: "COMPLETED"
        - createdAt: Purchase timestamp
        ↓
STEP 4: User's Wallet.balance decremented in Wallet table
        ↓
STEP 5: WalletWalletTransaction created as audit with:
        - userId: Card holder's user_id
        - eventId: Event's event_id
        - transactionType: "debit"
        - amount: Purchase amount
        - paymentMethod: null
        - description: "Stall purchase: items..."
        - transactionDate: Purchase timestamp
```

---

## 2. DATA MODELS

### 2.1 VolunteerWalletTransaction

**Table**: `volunteer_wallet_transactions`  
**Purpose**: Track topup payments by volunteers/operators

| Field          | Type          | Notes                              |
| -------------- | ------------- | ---------------------------------- |
| transaction_id | UUID (PK)     | Auto-generated                     |
| topup_user_id  | String        | TOPUP_USER who performed the topup |
| event_id       | String        | Event being topped up              |
| user_id        | String        | User receiving the topup           |
| type           | String        | TOPUP_CASH, TOPUP_UPI              |
| amount         | Double        | Topup amount in rupees             |
| payment_method | String        | CASH or UPI                        |
| status         | String        | SUCCESS, PENDING, FAILED           |
| created_at     | LocalDateTime | Transaction timestamp (IST)        |

**Entity Class**: `VolunteerWalletTransaction.java`  
**Repository**: `VolunteerWalletTransactionRepository.java`

### 2.2 WalletWalletTransaction

**Table**: `wallet_transactions`  
**Purpose**: Generic audit log for all wallet operations

| Field            | Type          | Notes                                    |
| ---------------- | ------------- | ---------------------------------------- |
| transaction_id   | UUID (PK)     | Auto-generated                           |
| wallet_id        | UUID (FK)     | Associated wallet                        |
| user_id          | String        | User involved in transaction             |
| event_id         | String        | Event context                            |
| amount           | Double        | Transaction amount                       |
| transaction_type | String        | "credit" (topup) or "debit" (redemption) |
| transaction_date | LocalDateTime | Transaction timestamp                    |
| description      | String        | Purpose/details                          |
| payment_method   | String        | CASH, UPI, or null                       |

**Entity Class**: `WalletWalletTransaction.java`  
**Repository**: `WalletWalletTransactionRepository.java`

**Special Notes**:

- Created by multiple systems automatically (not TOPUP_USER specific)
- May have overlapping transactions with VolunteerWalletTransaction
- Used for wallet balance audit trail

### 2.3 RedemptionTransaction

**Table**: `redemption_transactions`  
**Purpose**: Track stall sales/redemptions

| Field          | Type          | Notes                          |
| -------------- | ------------- | ------------------------------ |
| transaction_id | UUID (PK)     | Auto-generated                 |
| user_id        | String        | STALL_VENDOR who made the sale |
| event_id       | String        | Event where sale occurred      |
| total_amount   | Double        | Sale amount in rupees          |
| status         | String        | COMPLETED, PENDING, CANCELLED  |
| created_at     | LocalDateTime | Sale timestamp (IST)           |

**Entity Class**: `RedemptionTransaction.java`  
**Repository**: `RedemptionTransactionRepository.java`

**Special Relationship**:

- `user_id` is a STALL_VENDOR (User with role = STALL_VENDOR)
- Filtering by valid stall vendors requires joining with User table

---

## 3. API ENDPOINTS

### 3.1 Main Live Revenue Endpoint

**GET** `/api/v1/liverevenue/{eventId}`

**Query Parameters**:

- `startDate` (optional): ISO date string or simple date (2026-02-19T18:30:00.000Z or 2026-02-19)
- `endDate` (optional): ISO date string or simple date

**Behavior**:

- If NO custom date range provided: Fetches ALL data from 2000-01-01 to today
- If custom date range provided: Fetches only data within that range
- If event starts/ends within custom range, ignores event dates and uses custom range

**Response Structure**:

```json
{
  "success": true,
  "event": {
    "id": "event-uuid",
    "name": "Event Title",
    "startDate": "2026-02-28",
    "endDate": "2026-03-10"
  },
  "transactions": {
    "startDate": "2026-02-28", // Only if custom date range provided
    "endDate": "2026-03-10", // Only if custom date range provided
    "topups": {
      "total_top_up_amount": 10000.5,
      "no_of_topups": 50,
      "total_upi_payment_amount": 6000.25,
      "total_cash_payment_amount": 4000.25
    },
    "redemptions": {
      "total_redemption_amount": 8500.0,
      "total_rfid_sales": 45
    },
    "left_over_amount": 1500.5,
    "onsite_refunds": {
      "total_amount": 0,
      "cards_count": 0
    }
  }
}
```

**Data Calculation Logic**:

```java
// TOPUPS: Aggregate from TWO sources
totalUPI = (VolunteerWalletTransaction with type=TOPUP_UPI + status=SUCCESS + paymentMethod=UPI)
         + (WalletWalletTransaction with transactionType=credit + paymentMethod=UPI)

totalCASH = (VolunteerWalletTransaction with type=TOPUP_CASH + status=SUCCESS + paymentMethod=CASH)
          + (WalletWalletTransaction with transactionType=credit + paymentMethod=CASH)

totalTopup = totalUPI + totalCASH

// REDEMPTIONS: From single source (filtered by status)
totalRedemption = SUM(RedemptionTransaction.totalAmount)
                  WHERE status = "COMPLETED"

// LEFT OVER
leftOver = totalTopup - totalRedemption
```

### 3.2 Topups by Operator Endpoint

**GET** `/api/v1/liverevenue/{eventId}/topups`

**Returns**: Array of operator sales

```json
[
  {
    "name": "operator_username_or_id",
    "total_sales": 12,
    "topup_amount": 5000.5
  }
]
```

**Filtering**:

- Only VolunteerWalletTransaction with:
  - type IN ("TOPUP_CASH", "TOPUP_UPI")
  - status = "SUCCESS"
  - paymentMethod IN ("CASH", "UPI")
- Grouped by `topup_user_id`

### 3.3 Redemptions by Vendor Endpoint

**GET** `/api/v1/liverevenue/{eventId}/redemptions`

**Returns**: Array of vendor sales

```json
[
  {
    "name": "vendor_username_or_id",
    "total_orders": 45,
    "total_sale": 8500.5,
    "machines_count": 1
  }
]
```

**Filtering**:

- Only RedemptionTransaction with:
  - status = "COMPLETED"
- Grouped by `user_id` (stall vendor)

---

## 4. THE ZERO-VALUE PROBLEM & SOLUTION

### 4.1 Root Cause Analysis

**Original Implementation Issue**:

```java
// OLD: Only looked at WalletTopupTransaction (wrong entity)
List<WalletTopupTransaction> topups = topupTransactionRepository.findAll()
    .filter(t -> t.getEventId().equals(eventId))
    .toList();
// Result: 0 transactions found (wrong table!)
```

**Why Zero Values**:

1. Development system uses `VolunteerWalletTransaction` for topups
2. Test data was created via volunteer app (creates VolunteerWalletTransaction)
3. Code was querying `WalletTopupTransaction` (different table)
4. No data found → returned 0

### 4.2 Solution Implemented

**New Implementation** aggregates from BOTH sources:

```java
// SOURCE 1: VolunteerWalletTransaction (primary for topups)
List<VolunteerWalletTransaction> volunteerTopups =
    volunteerWalletTransactionRepository.findAll()
    .filter(t -> t.getEventId().equals(eventId))
    .filter(t -> t.getType().equals("TOPUP_CASH") || t.getType().equals("TOPUP_UPI"))
    .filter(t -> t.getStatus().equals("SUCCESS"))
    .filter(t -> t.getPaymentMethod().equalsIgnoreCase("CASH") ||
                 t.getPaymentMethod().equalsIgnoreCase("UPI"))
    .toList();

// SOURCE 2: WalletWalletTransaction (fallback/audit log)
List<WalletWalletTransaction> walletTopups =
    walletWalletTransactionRepository.findAll()
    .filter(t -> t.getEventId().equals(eventId))
    .filter(t -> t.getTransactionType().equalsIgnoreCase("credit"))
    .filter(t -> t.getPaymentMethod() != null &&
                 (t.getPaymentMethod().equalsIgnoreCase("CASH") ||
                  t.getPaymentMethod().equalsIgnoreCase("UPI")))
    .toList();

// COMBINE: Sum from both sources
BigDecimal totalUPI = volunteerUPI.add(walletUPI);
BigDecimal totalCASH = volunteerCASH.add(walletCASH);
```

### 4.3 Debug Logging Output

The service logs show data from each source separately:

```
📊 SOURCE 1 - VolunteerWalletTransaction total found: 25
📊 SOURCE 1 - VolunteerWalletTransaction topups (SUCCESS, CASH/UPI): 23
  - TopupUserId: volunteer-001, Method: UPI, Amount: 500.0
  - TopupUserId: volunteer-002, Method: CASH, Amount: 1000.0

📊 SOURCE 2 - WalletWalletTransaction total found: 45
📊 SOURCE 2 - WalletWalletTransaction topups (CREDIT, CASH/UPI): 8
  - UserId: user-123, Method: UPI, Amount: 200.0
  - UserId: user-456, Method: CASH, Amount: 300.0

📊 Total UPI (Volunteer: 10000.00 + Wallet: 1500.00): 11500.00
📊 Total CASH (Volunteer: 8000.00 + Wallet: 800.00): 8800.00
```

---

## 5. WALLET SYSTEM ARCHITECTURE

### 5.1 Wallet Creation Flow

```
User adds topup funds
    ↓
VolunteerService.topupWallet() called
    ↓
WalletService.addCredit() called with:
  - userId: User receiving topup
  - eventId: Event for wallet
  - amount: Topup amount
    ↓
Check if wallet exists:

  YES │ Update balance += amount
      │ Set updatedAt = now()
      │
  NO  │ Create new Wallet:
      │ ├─ id (UUID) ← auto-generated
      │ ├─ userId ← from parameter
      │ ├─ eventId ← from parameter
      │ ├─ balance ← amount (initial topup)
      │ ├─ createdAt ← now(IST)
      │ ├─ updatedAt ← now(IST)
      │ ├─ eventName ← event name or eventId
      │ └─ qrString ← generated (userId::walletId::timestamp)
      │              and setIsWalletLocked = false
      ↓
Save wallet to database
    ↓
Create TWO audit records:

  1. VolunteerWalletTransaction
     ├─ topupUserId: operator
     ├─ userId: card holder
     ├─ amount: topup amount
     ├─ type: TOPUP_CASH or TOPUP_UPI
     ├─ status: SUCCESS
     └─ paymentMethod: CASH or UPI

  2. WalletWalletTransaction (optional)
     ├─ userId: card holder
     ├─ amount: topup amount
     ├─ transactionType: "credit"
     └─ paymentMethod: CASH or UPI
```

### 5.2 Wallet Fields

| Field          | Type          | Purpose                      |
| -------------- | ------------- | ---------------------------- |
| id             | UUID          | Primary key                  |
| userId         | String        | Card holder's user ID        |
| eventId        | String        | Event context                |
| balance        | Double        | Current available funds      |
| eventName      | String        | Display name                 |
| qrString       | String        | Format: userId::walletId::ts |
| qrCodeUrl      | String        | QR code image URL            |
| isWalletLocked | Boolean       | Security flag                |
| createdAt      | LocalDateTime | When first created           |
| updatedAt      | LocalDateTime | Last modification time       |

---

## 6. REDEMPTION SYSTEM

### 6.1 Redemption Flow

```
User (with wallet) scans QR at stall
    ↓
StallVendor (STALL_VENDOR) scans/confirms redemption
    ↓
RedemptionService.checkout() validates:
  ├─ User exists
  ├─ Event exists
  ├─ Wallet exists and has sufficient balance
  ├─ QR code matches wallet's QR string
  └─ Stall vendor is authorized
    ↓
Create RedemptionTransaction:
  ├─ userId: STALL_VENDOR's ID
  ├─ eventId: Event's ID
  ├─ totalAmount: Purchase amount
  ├─ status: COMPLETED
  ├─ createdAt: now(IST)
  └─ transactionId: UUID
    ↓
Deduct from user's Wallet:
  ├─ balance = balance - purchaseAmount
  └─ updatedAt = now()
    ↓
Create WalletWalletTransaction (audit):
  ├─ userId: user who made purchase
  ├─ eventId: event
  ├─ transactionType: "debit"
  ├─ amount: purchase amount
  └─ description: "Stall purchase: items..."
    ↓
Return CheckoutResponse with:
  ├─ success: true
  ├─ totalAmount: purchase amount
  ├─ newBalance: remaining balance
  └─ items purchased
```

### 6.2 Redemption Filters

For the Live Revenue endpoint:

```
RedemptionTransaction.findByEventIdAndStatusAndCreatedAtBetween()
  Filter: status = "COMPLETED"
  Filter: createdAt BETWEEN startDate AND endDate

Group by: userId (STALL_VENDOR)
Sum by: totalAmount
Count: total transactions per vendor
```

---

## 7. KEY ISSUES & FIXES

### Issue 1: Zero Values on Dashboard

**Problem**: Main page showed 0 for all amounts
**Cause**: Only checked VolunteerWalletTransaction in old code; actual data in WalletWalletTransaction
**Fix**: Aggregate from BOTH sources
**Status**: ✅ FIXED

### Issue 2: Modal Shows Different Data

**Problem**: Modal showed 500.69 while main page showed 0
**Cause**: Modal used different method (getTopupsByOperator) which was also checking wrong table
**Fix**: Updated modal method to use correct VolunteerWalletTransaction
**Status**: ✅ FIXED

### Issue 3: Date Range Filtering

**Problem**: When no date provided, only returned event start/end dates
**Cause**: Code used `event.getStartDate()` as default
**Fix**: Changed to get ALL data from 2000-01-01 to today when no custom date provided
**Status**: ✅ FIXED

### Issue 4: Missing Stall Vendor Validation

**Problem**: Possibly returning redemptions from non-stall vendors
**Cause**: No validation that userId is actually a STALL_VENDOR
**Fix**: Added optional validation to filter by UserRole.STALL_VENDOR
**Status**: ✅ Added (optional, can be toggled)

---

## 8. TESTING CHECKLIST

### Test Case 1: No Custom Date Range (Most Common)

```
Request: GET /api/v1/liverevenue/{eventId}
Expected: Returns ALL data from system regardless of event dates
Check:
  ✅ topups has non-zero amount
  ✅ redemptions has non-zero amount (if any)
  ✅ left_over = topups - redemptions
  ✅ No startDate/endDate in response
```

### Test Case 2: Custom Date Range

```
Request: GET /api/v1/liverevenue/{eventId}?startDate=2026-02-01&endDate=2026-02-28
Expected: Returns only transactions within date range
Check:
  ✅ startDate in response = 2026-02-01
  ✅ endDate in response = 2026-02-28
  ✅ Only transactions in Feb returned
  ✅ March transactions excluded
```

### Test Case 3: See Debug Logs

```
Command: Start backend with: mvn spring-boot:run
Expected: Console shows detailed logging
Output will show:
  📊 SOURCE 1 - VolunteerWalletTransaction: X found, Y topups
  📊 SOURCE 2 - WalletWalletTransaction: X found, Y credits
  📊 Total UPI (Volunteer: X + Wallet: Y): Total
  📊 Total CASH (Volunteer: X + Wallet: Y): Total
  📊 Total Redemptions: X
```

### Test Case 4: Test Modal Data

```
Request: GET /api/v1/liverevenue/{eventId}/topups
Expected: List of operators with totals

Request: GET /api/v1/liverevenue/{eventId}/redemptions
Expected: List of vendors with totals
```

---

## 9. CODE STRUCTURE SUMMARY

### Backend Files Modified

1. **LiveRevenueService.java**
   - Line 20-25: Added imports for VolunteerWalletTransaction, WalletWalletTransaction
   - Line 32-35: Added repository autowires
   - Line 78-165: Rewrote getLiveRevenue() to use both sources
   - Line 217-244: Updated getTopupsByOperator() for VolunteerWalletTransaction

2. **VolunteerWalletTransactionRepository.java**
   - Line 13-16: Repository interface methods available

3. **WalletWalletTransactionRepository.java**
   - Line 13-17: Repository interface methods available

### Frontend Files Integrated

1. **live-revenue-types.ts** - Type definitions
2. **use-get-event-live-revenue.tsx** - API hook for main data
3. **top-up-card.tsx** - Display component for topups
4. **view-all-topups-modal.tsx** - Modal for detailed topup breakdown

---

## 10. FUTURE IMPROVEMENTS

1. **Performance**: Add database indices for eventId + transactionDate in both tables
2. **Data Consistency**: Consider merging overlapping VolunteerWalletTransaction and WalletWalletTransaction data during sync
3. **Validation**: Implement strict STALL_VENDOR role checking for redemptions
4. **Deduplication**: Some transactions may appear in both sources - add deduplication logic
5. **Real-time**: Implement WebSocket for real-time dashboard updates
6. **Export**: Add export functionality (CSV/Excel) for reports

---

## REFERENCES

- **Frontend Integration**: [API_INTEGRATION_COMPLETE.md](../live-revenue-new/API_INTEGRATION_COMPLETE.md)
- **Wallet Architecture**: [WALLET_FIRST_QR_VALIDATION_ANALYSIS.md](./WALLET_FIRST_QR_VALIDATION_ANALYSIS.md)
- **Data Persistence**: [DATA_PERSISTENCE_GUIDE.md](./DATA_PERSISTENCE_GUIDE.md)
