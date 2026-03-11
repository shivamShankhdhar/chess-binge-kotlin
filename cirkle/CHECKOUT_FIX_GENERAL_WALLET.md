# Checkout "General Wallet Not Found" - FIXED ✅

## Problem

Checkout was failing with error: `"GENERAL wallet not found for user"`

This happened because the code was:

1. Trying to find `Wallet` records in the wallets table
2. Throwing an error if GENERAL wallet didn't exist
3. Not handling cases where a user had balance but no GENERAL wallet record

## Root Cause

The checkout logic was split between two sources:

- **Balance validation**: Reading from `TopupBalance` table (doesn't account for deductions)
- **Balance deduction**: Trying to update `Wallet` table (requiring records to exist)
- **Actual transactions**: Creating `WalletWalletTransaction` records

This inconsistency caused:

- Checkout to fail if GENERAL wallet record didn't exist
- Balance to be out of sync (TopupBalance vs wallet_transactions)

## Solution: Consolidate to Single Source of Truth

### Fix 1: Balance Check (Lines 320-336)

**Changed FROM:**

```java
// Using TopupBalance.availableBalance (doesn't account for deductions)
double eventAvailable = eventTopup.getAvailableBalance();
double generalAvailable = generalTopupOpt.map(TopupBalance::getAvailableBalance).orElse(0.0);
double totalAvailable = eventAvailable + generalAvailable;
```

**Changed TO:**

```java
// Using wallet_transactions.finalBalance (accounts for all topups AND deductions)
Double eventBalance = walletService.getLatestBalance(request.getUserId(), request.getEventId());
Double generalBalance = walletService.getLatestBalance(request.getUserId(), "GENERAL");
double totalAvailable = eventBalance + generalBalance;
```

### Fix 2: Balance Deduction (Lines 400-445)

**Changed FROM:**

```java
// Trying to deduct from Wallet table - fails if record doesn't exist
Optional<Wallet> debitGeneralWalletOpt = walletRepository.findByUserIdAndEventId(
    request.getUserId(), "GENERAL");
if (debitGeneralWalletOpt.isPresent()) {
    Wallet generalWallet = debitGeneralWalletOpt.get();
    deductedFromGeneralWallet = Math.min(walletRemaining, generalWallet.getBalance());
    // update generalWallet.setBalance()
} else {
    throw new RuntimeException("GENERAL wallet not found for user");  // ❌ ERROR
}
```

**Changed TO:**

```java
// Just calculate amounts to deduct - no Wallet table updates
Double eventBalance = walletService.getLatestBalance(request.getUserId(), request.getEventId());
Double generalBalance = walletService.getLatestBalance(request.getUserId(), "GENERAL");

if (eventBalance > 0 && walletRemaining > 0) {
    deductedFromEventWallet = Math.min(walletRemaining, eventBalance);
    walletRemaining -= deductedFromEventWallet;
}

if (walletRemaining > 0) {
    if (generalBalance > 0) {
        deductedFromGeneralWallet = Math.min(walletRemaining, generalBalance);
    } else {
        throw new RuntimeException("Insufficient wallet balance for this purchase");
    }
}
```

## Architecture After Fixes

### Data Flow

```
1. Balance Check (via wallet_transactions)
   └─> getLatestBalance(userId, eventId)
   └─> getLatestBalance(userId, GENERAL)
   └─> Returns finalBalance from wallet_transactions

2. Calculate deduction amounts
   └─> Based on available balance from step 1
   └─> No database updates yet

3. Create transaction records
   └─> WalletWalletTransaction (DEBIT) with finalBalance
   └─> Records settled in wallet_transactions table
   └─> Balance history preserved

4. Optional: Update tracking tables
   └─> TopupBalance (for quick lookups)
   └─> Wallet.balance (for UI caching only)
```

### Key Changes

| Component            | Before                                    | After                            | Status   |
| -------------------- | ----------------------------------------- | -------------------------------- | -------- |
| Balance validation   | TopupBalance.availableBalance             | wallet_transactions.finalBalance | ✅ FIXED |
| Balance deduction    | Wallet table update (requires record)     | wallet_transactions record only  | ✅ FIXED |
| Wallet lock check    | Still checks optional GENERAL wallet      | Still optional (no change)       | ✅ OK    |
| Transaction creation | WalletWalletTransaction with finalBalance | Same, but guaranteed consistent  | ✅ OK    |

## Checkout Flow Now

1. **Validate user/event exist** ✅
2. **Check minimum topup (₹500)** - Uses TopupBalance ✅
3. **Check balance from wallet_transactions**
   - Event + General balance
   - Accounts for all topups AND deductions ✅
4. **Check wallet lock status** - Optional, if record exists ✅
5. **Calculate deduction amounts** - No DB updates ✅
6. **Create WalletWalletTransaction records**
   - Event DEBIT (if amount > 0)
   - General DEBIT (if overflow > 0)
   - Both have finalBalance calculated correctly ✅
7. **Update TopupBalance** - For tracking/reconciliation ✅

## No More Wallet Table Direct Updates

**What changed:**

- ❌ Removed: Direct `walletRepository.save()` during checkout deduction
- ❌ Removed: Requirement for GENERAL `Wallet` record to exist
- ✅ Added: All deductions tracked via `WalletWalletTransaction`
- ✅ Added: Balance checks use `wallet_transactions.finalBalance`

**What stayed the same:**

- Wallet lock check (optional)
- TopupBalance updates (for tracking)
- Transaction structure (redeemption records)

## Files Modified

### d:\Projects\cirkle-backend-common\src\main\java\com\razespace\cirklebackend\stall\service\RedemptionService.java

**Section 1: Balance Check (Lines 320-336)**

- Changed from TopupBalance to wallet_transactions.finalBalance
- Uses `walletService.getLatestBalance()`

**Section 2: Balance Deduction (Lines 400-445)**

- Removed Wallet table lookups
- Removed Wallet.balance updates
- Removed "GENERAL wallet not found" error
- Now calculates deduction amounts from available balance
- Creates WalletWalletTransaction records instead

## Testing Checklist

1. **User with topup but no GENERAL Wallet record**
   - [ ] Topup for Event A ₹500
   - [ ] Don't create separate GENERAL wallet
   - [ ] Checkout should work ✅

2. **Checkout with event + general deduction**
   - [ ] Event balance = ₹300
   - [ ] General balance = ₹200
   - [ ] Checkout for ₹400
   - [ ] Should deduct ₹300 from event, ₹100 from general ✅
   - [ ] Should NOT throw "GENERAL wallet not found" ✅

3. **Insufficient balance**
   - [ ] Event = ₹100, General = ₹50
   - [ ] Checkout for ₹200
   - [ ] Should throw "Insufficient balance" (not "wallet not found") ✅

4. **Balance consistency**
   - [ ] Check wallet_transactions has DEBIT records
   - [ ] finalBalance calculated correctly
   - [ ] Web overview shows updated balance ✅

## Impact

- ✅ Checkout no longer fails on missing GENERAL wallet record
- ✅ All balance calculations now use wallet_transactions (single source of truth)
- ✅ Better error messages (insufficient balance vs wallet not found)
- ✅ Simpler logic (no orphaned Wallet records needed)
- ✅ Consistent with topup and QR validation flows
