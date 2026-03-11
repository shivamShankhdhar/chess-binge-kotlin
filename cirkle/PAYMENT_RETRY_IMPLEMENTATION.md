# Payment Retry & OTP Expiry Fix - Implementation Summary

## Overview

Fixed critical payment retry tracking issues where attempt counters were resetting and expiry times were not being preserved globally.

## Problem Statement

When user cancelled payment multiple times:

- **Retry attempts showed 0 instead of incrementing (should be 1/2)**
- **Max retries showed 0 instead of 2**
- **Expires In time reset** every cancellation instead of preserving original expiry
- **No global server-side validation** of attempts
- Not production-ready for deployment

## Solution Architecture

### Server-Side (Global State Management)

The payment retry state is now **exclusively managed on the backend** with database persistence.

**Payment Retry Workflow:**

```
1. First Payment Cancellation
   ├─ Check if PENDING retry exists for this order
   ├─ If exists and not expired → Reuse (preserve count & expiry)
   └─ If not exists → Create with retryCount=0, expiresAt=now+20min

2. Subsequent Cancellations (same order)
   ├─ Check if PENDING retry exists
   ├─ Found → Return existing (time countdown continues)
   └─ Server response: accurate retryCount, remaining seconds

3. User Attempts Retry
   ├─ POST /retry/attempt
   ├─ Backend increments retryCount
   ├─ Returns updated: retryCount=1, timeRemaining=<calculated>
   └─ If retryCount >= maxRetries (2) → Mark FAILED

4. Session Expiry (20 minutes)
   ├─ Scheduled job runs every 60s
   ├─ Checks expiresAt timestamp
   └─ Marks as EXPIRED if past deadline
```

### Backend Implementation

**File: PaymentRetryService.java**

- Modified `createRetry()` to check for existing PENDING retry first
- If found and not expired: return existing record (preserve attempt count and expiry)
- If not found: create new with retryCount=0, maxRetries=2
- Changed maxRetries from 3 to 2 per requirement

**File: WalletController.java - POST /wallet/retry/create**

- Removed hardcoded values: was returning retryCount=0, maxRetries=3, timeRemaining=1200
- Now returns actual DB values: `retry.getRetryCount()`, `retry.getMaxRetries()`
- Calculates remaining time from actual expiresAt: `ChronoUnit.SECONDS.between(now, expiresAt)`

**File: PaymentRetryRepository.java**

- Added `findByPaymentOrderIdAndStatus()` query
- Enables checking if active retry exists before creating new one

### Frontend Implementation

**File: topup-screen.tsx**

**1. Payment Cancellation Handler**

```
Previous (WRONG):
  await createRetry(orderId, eventId);
  showModal('retry', 'Payment Cancelled', msg, amount, 1, 3, 1200);
  // Hardcoded values, always showed 1/3 with 20:00 time

New (CORRECT):
  const createdRetry = await createRetry(orderId, eventId);
  if (createdRetry) {
    showModal('retry', 'Payment Cancelled', msg, amount,
      createdRetry.retryCount,              // Server DB value
      createdRetry.maxRetries,              // Server DB value (2)
      createdRetry.timeRemainingSeconds     // Calculated on server
    );
  }
```

**2. Payment Failure Handler**

- Similar update: uses server response data instead of hardcoded values
- Shows "Attempt 0/2" on first failure, "Attempt 1/2" on second, etc.

**3. Verification Failure Handler**

- Fetches updated retry status from server: `await fetchRetryStatus()`
- Shows accurate attempt count that was incremented by `/retry/attempt`
- Example flow:
  - User cancels: shows "Attempt 0/2, Expires 19:XX"
  - User retries: shows "Attempt 1/2, Expires 18:XX"

**File: usePaymentRetry.ts (Hook)**

- Modified `fetchRetryStatus()` to return the retry data
- Changed return type from `void` to `Promise<RetryData | null>`
- Now callers can use returned data for UI updates

## Key Features

✅ **Global State Tracking**: Retry state persists in database, not client memory
✅ **Expiry Preservation**: Original expiresAt timestamp preserved across cancellations
✅ **Accurate Counters**: Attempt count retrieved from DB, not reset on each call
✅ **Server-Side Validation**: Backend enforces max 2 attempts, not frontend
✅ **Time Accuracy**: Remaining seconds calculated on server, immune to client clock issues
✅ **Automatic Cleanup**: Scheduled job expires old retries every 60 seconds
✅ **Security**: User ID and order ID verified for each attempt
✅ **Consistent UI**: Shows actual attempt count from server (0/2, 1/2, etc.)

## Production Readiness Checklist

- [x] Server-side validation implemented
- [x] Database persistence (PaymentRetry entity)
- [x] No client-side state assumptions
- [x] Scheduled cleanup job for expiry
- [x] User authorization verified
- [x] Type-safe implementation (TypeScript)
- [x] No compilation errors
- [x] Backward compatible (existing records reused)

## Testing Scenarios

**Scenario 1: Multiple Cancellations**

1. User taps Pay → Razorpay opens
2. User closes payment modal → Cancel (shows 0/2, Expires 20:00)
3. User taps Pay again → Razorpay opens
4. User closes payment modal → Cancel (shows 0/2, Expires 19:XX) ✅
   - NOT reset to 20:00
   - Same retry reused from DB

**Scenario 2: Retry Attempts**

1. Cancel payment (shows 0/2)
2. Retry (shows loading, then 1/2 after payment sent)
3. If 2nd attempt also fails → shows 1/2 cannot retry (max exceeded)

**Scenario 3: Expiry**

1. Cancel payment (20 min window starts)
2. Wait 20 minutes
3. Attempt to retry (shows "Retry window expired")

## Deployment Steps

1. **Backend**:
   - Build: `mvn clean package`
   - Deploy updated WalletController + PaymentRetryService
   - Ensure PaymentRetryRepository changes compiled

2. **Frontend**:
   - Build: `bunx expo run:android` (or iOS)
   - Verify no console errors in payment flow

3. **Database**:
   - No migrations needed (using existing PaymentRetry table)
   - Existing PENDING retries will work with new logic

4. **Verification**:
   - Run through all testing scenarios
   - Check server logs for "reused existing retry" vs "created new retry"
   - Verify attempt counter increments correctly

## Rollback Plan

If issues found:

1. Revert WalletController to version without server-calculated times
2. Revert PaymentRetryService to old createRetry (will create new records per cancellation)
3. Revert frontend to hardcoded values
4. This maintains functionality but loses global state tracking

## Files Changed

**Backend**:

- ✅ `PaymentRetryService.java` - createRetry() logic
- ✅ `WalletController.java` - /retry/create response values
- ✅ `PaymentRetryRepository.java` - Added query method

**Frontend**:

- ✅ `topup-screen.tsx` - Payment handling + retry flows
- ✅ `usePaymentRetry.ts` - fetchRetryStatus return type

**Documentation**:

- ✅ `PAYMENT_RETRY_FIX.md` - Detailed technical documentation
