# Balance Sync Fix - Complete Summary

## Problem Identified

Three different balance values were being displayed across the system:

- **Web app**: ₹613 (legacy data from Wallet.balance table)
- **QR validation**: ₹115 (stale wallet table read)
- **Redemption checkout**: ₹730 (incorrect calculation)

**Root Cause**: Multiple parts of the backend were reading from different sources:

1. Some reading from `Wallet.balance` (stale, not updated consistently)
2. Some reading from `wallet_transactions.finalBalance` (correct, source of truth)
3. Some doing manual calculations from transactions

## Critical Fixes Applied

### 1. **WalletService.java** - Consolidated Balance Reads

**File**: `cirkle-backend-common/src/main/java/com/razespace/cirklebackend/wallet/service/WalletService.java`

#### Fix 1: `getWalletBalance()` method

```java
// BEFORE (read stale Wallet.balance):
public Double getWalletBalance(String userId, String eventId) {
    Optional<Wallet> walletOpt = walletRepository.findByUserIdAndEventId(userId, eventId);
    return walletOpt.map(Wallet::getBalance).orElse(0.0);  // ❌ STALE
}

// AFTER (read from wallet_transactions):
public Double getWalletBalance(String userId, String eventId) {
    return getLatestBalance(userId, eventId);  // ✅ From wallet_transactions.finalBalance
}
```

#### Fix 2: `getGeneralWalletBalance()` method

```java
// BEFORE (read stale Wallet.balance):
public Double getGeneralWalletBalance(String userId) {
    Optional<Wallet> walletOpt = walletRepository.findByUserIdAndEventId(userId, "GENERAL");
    return walletOpt.map(Wallet::getBalance).orElse(0.0);  // ❌ STALE
}

// AFTER (read from wallet_transactions):
public Double getGeneralWalletBalance(String userId) {
    return getLatestBalance(userId, "GENERAL");  // ✅ From wallet_transactions.finalBalance
}
```

### 2. **WalletController.java** - QR Validation Balance Fix

**File**: `cirkle-backend-common/src/main/java/com/razespace/cirklebackend/wallet/controller/WalletController.java`
**Lines**: 240-260 (validateByQrString method)

```java
// BEFORE (read stale Wallet.balance):
double totalBalance = 0.0;
try {
    Optional<Wallet> eventWalletOpt = walletRepository.findByUserIdAndEventId(userIdFromWallet, requestEventId);
    if (eventWalletOpt.isPresent()) {
        Wallet eventWallet = eventWalletOpt.get();
        totalBalance = eventWallet.getBalance() != null ? eventWallet.getBalance() : 0.0;
    }
} catch (Exception e) {
    // If wallet lookup fails...
}

// AFTER (read from wallet_transactions):
double totalBalance = walletService.getLatestBalance(userIdFromWallet, requestEventId);
```

### 3. **RedemptionService.java** - Checkout Balance Calculation Fix

**File**: `cirkle-backend-common/src/main/java/com/razespace/cirklebackend/stall/service/RedemptionService.java`
**Lines**: 260-289 (checkout method)

```java
// BEFORE (stale read + manual recalculation):
double totalWalletBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
// Then manually looping through all transactions to recalculate...

// AFTER (single source of truth):
Double eventBalance = walletService.getLatestBalance(request.getUserId(), request.getEventId());
Double generalBalance = walletService.getLatestBalance(request.getUserId(), "GENERAL");
double totalWalletBalance = eventBalance + generalBalance;
```

## Data Flow After Fixes

### Topup Flow ✅

1. Volunteer topups ₹500 for Event A
2. Creates `WalletWalletTransaction` (CREDIT) with `finalBalance=500`
3. Updates `Wallet.balance = 500` (caching only)

### QR Validation Flow ✅ (NOW FIXED)

1. Endpoint calls `walletService.getLatestBalance(userId, eventId)`
2. Reads from `wallet_transactions.finalBalance` (always latest)
3. Returns ₹500 for Event A (correct, event-specific)

### Redemption Checkout Flow ✅ (NOW FIXED)

1. Endpoint calls `getLatestBalance(userId, eventId) + getLatestBalance(userId, "GENERAL")`
2. Reads from `wallet_transactions.finalBalance` (always latest)
3. Calculates correct total = Event Balance + General Balance
4. Deductions apply to both if needed

### Web Overview Flow ✅

1. Endpoint calls `getUserWalletOverview(userId)`
2. Uses `wallet_transactions.finalBalance` for each event (source of truth)
3. Returns per-event breakdown:
   - Event A: ₹500
   - Event B: ₹100
   - General: ₹13
4. Web app displays as needed (total or per-event)

## Source of Truth Consolidation

| Component              | Method                             | Source                             | Status               |
| ---------------------- | ---------------------------------- | ---------------------------------- | -------------------- |
| General balance lookup | `getGeneralWalletBalance()`        | `wallet_transactions.finalBalance` | ✅ FIXED             |
| Event balance lookup   | `getWalletBalance()`               | `wallet_transactions.finalBalance` | ✅ FIXED             |
| QR validation          | `getLatestBalance()`               | `wallet_transactions.finalBalance` | ✅ FIXED             |
| Redemption checkout    | `getLatestBalance()`               | `wallet_transactions.finalBalance` | ✅ FIXED             |
| Web overview           | `wallet_transactions.finalBalance` | `wallet_transactions.finalBalance` | ✅ (already correct) |

## Important Notes

### Wallet.balance Field

- **Purpose**: UI caching only, for quick display
- **Not used**: For balance calculations anymore (all reads now from wallet_transactions)
- **Still updated**: During topup/deduction operations
- **Status**: Can be ignored for logic, only updated for historical reference

### wallet_transactions.finalBalance Field

- **Purpose**: Source of truth for all balance calculations
- **Created**: During every transaction (topup, deduction)
- **Used by**: All balance calculation methods (getLatestBalance, getWalletBalance, getGeneralWalletBalance)
- **Status**: Single source of truth ✅

### Database Impact

- ✅ No schema changes needed (finalBalance already exists)
- ✅ No data migration needed (both tables updated simultaneously)
- ✅ Backward compatible (still updates Wallet.balance for legacy systems)

## Testing Checklist

After these fixes, verify:

1. **Topup Flow**
   - [ ] Do topup for Event A ₹500
   - [ ] Check wallet_transactions has CREDIT with finalBalance=500
   - [ ] Check Wallet.balance updated to 500

2. **QR Validation**
   - [ ] Call `/api/wallet/validate-qr` for Event A
   - [ ] Should return balance=500 (event-specific)
   - [ ] Should NOT include other events' balance

3. **Redemption Checkout**
   - [ ] Redemption for ₹200 on Event A
   - [ ] Should check Event A balance (₹500) + General balance
   - [ ] Should successfully deduct ₹200
   - [ ] Final balance should be ₹300 for Event A

4. **Web Overview**
   - [ ] Should show Event A: ₹300, General: ₹13, Total: ₹313
   - [ ] Should NOT show sum of ALL other events

5. **Balance Consistency**
   - [ ] Web app shows ₹313 (Event A ₹300 + General ₹13)
   - [ ] QR validation shows ₹300 for Event A (event-specific)
   - [ ] Redemption calculates ₹313 (same as web) ✅

## Files Modified

1. `cirkle-backend-common/src/main/java/com/razespace/cirklebackend/wallet/service/WalletService.java`
   - `getWalletBalance()` - Line 41-44
   - `getGeneralWalletBalance()` - Line 336-339

2. `cirkle-backend-common/src/main/java/com/razespace/cirklebackend/wallet/controller/WalletController.java`
   - `validateByQrString()` - Lines 240-260

3. `cirkle-backend-common/src/main/java/com/razespace/cirklebackend/stall/service/RedemptionService.java`
   - `checkout()` - Lines 260-289

## Architecture Decision

**All balance calculations now follow this hierarchy:**

```
wallet_transactions.finalBalance (Source of Truth)
    ↓
getLatestBalance(userId, eventId) [Service method]
    ↓
getWalletBalance() / getGeneralWalletBalance() [Service methods]
    ↓
All endpoints (QR validation, Redemption, Web overview)
```

This ensures:

- ✅ Single source of truth
- ✅ Consistent data across all endpoints
- ✅ No stale data reads
- ✅ No manual recalculations
- ✅ Automatic consistency with transactions

## Next Steps

1. Recompile and test the three fixed flows
2. Verify numbers are consistent across web, QR, and redemption
3. Monitor wallet_transactions table to ensure finalBalance is always correct
4. Consider adding validation that event-specific balance ≠ sum of all events
