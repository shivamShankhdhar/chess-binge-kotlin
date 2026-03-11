# Robust Payment Detection System - Complete Implementation

## Overview

Implemented a production-grade payment detection system similar to IRCTC with:

- ✅ **Razorpay Webhooks** - Real-time payment notifications
- ✅ **Payment Timer** - 15-minute countdown for payment completion
- ✅ **Automatic Detection** - No manual confirmation needed (fallback available)
- ✅ **Order Expiry** - Auto-cancels if payment not received in time

## Architecture

### 1. Webhook Integration (Backend)

**File:** `RazorpayWebhookController.java`

- **Endpoint:** `POST /api/webhook/razorpay/payment`
- **Function:** Receives real-time payment notifications from Razorpay
- **Events Handled:**
  - `payment.authorized` - Payment authorized by bank
  - `payment.captured` - Payment successfully captured ✅ MAIN EVENT
  - `payment.failed` - Payment failed
  - `order.paid` - Order fully paid

**Security:**

- HMAC-SHA256 signature verification
- Ensures webhook comes from Razorpay, not attackers
- Requires `RAZORPAY_WEBHOOK_SECRET` environment variable

**Flow:**

```
Customer pays via UPI/Card
    ↓
Razorpay processes payment
    ↓
Razorpay calls webhook endpoint
    ↓
Backend verifies signature
    ↓
Backend updates order status to COMPLETED
    ↓
Frontend polls and detects completion
```

### 2. Frontend Timer & Auto-Detection

**File:** `recharge-wallet.tsx`

**Features:**

1. **Countdown Timer** - 15 minutes (900 seconds)
   - Shows MM:SS format
   - Updates every second
   - Uses hourglass icon
   - Changes color to red when expired

2. **Real-time Polling**
   - Polls every 2 seconds via `checkPaymentStatus` API
   - Detects when backend marks order as COMPLETED
   - Auto-closes modal on success
   - Shows success animation

3. **Order Expiry Handling**
   - Automatically stops polling after 15 minutes
   - Shows "Payment Expired" message
   - User can close and try again
   - Cleans up timers and intervals properly

4. **Fallback Confirmation Button**
   - For direct UPI QR codes
   - Manual "Confirm Payment Received" button
   - Only shown when using UPI QR
   - Backup if webhook fails

### 3. Backend Payment Status Management

**Service Method:** `updatePaymentOrderStatus()`

Called by webhook with parameters:

- `orderId` - Razorpay order ID
- `paymentId` - Razorpay payment ID
- `status` - COMPLETED, FAILED, AUTHORIZED

**Actions:**

1. Finds order in database
2. Updates status based on webhook event
3. Stores payment ID and verification timestamp
4. Logs transaction

## Payment Flow Sequence

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. PAYMENT INITIATED                                            │
│    ├─ Frontend calls /wallet/initiate-upi                      │
│    ├─ Backend creates Razorpay order                           │
│    ├─ Backend generates UPI QR code                            │
│    └─ Frontend receives order ID & shows QR modal              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. TIMER STARTED                                                │
│    ├─ Frontend starts 15-minute countdown timer                │
│    ├─ Frontend starts polling /check-payment-status every 2s   │
│    └─ User sees: MM:SS timer counting down                     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. CUSTOMER SCANS & PAYS                                        │
│    ├─ Customer scans QR with Google Pay/PhonePe               │
│    ├─ Customer completes payment in UPI app                    │
│    └─ Bank processes payment to merchant account               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. PAYMENT WEBHOOK (Real-time)                                  │
│    ├─ Razorpay calls webhook endpoint                          │
│    ├─ Backend verifies signature                               │
│    ├─ Backend updates order status → COMPLETED                │
│    ├─ Backend creates wallet transaction                       │
│    └─ Backend updates wallet balance                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. FRONTEND DETECTS (Polling)                                   │
│    ├─ Frontend polls and sees COMPLETED status                 │
│    ├─ Frontend calls /verify-upi (optional)                    │
│    ├─ Modal closes automatically                               │
│    ├─ Success animation shows                                  │
│    └─ Timer stops                                              │
└─────────────────────────────────────────────────────────────────┘
```

## Configuration

### Environment Variables

**Backend:**

```bash
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret_from_dashboard

UPI_MERCHANT_ID=your_actual_merchant_upi@bankcode
UPI_MERCHANT_NAME=Your Business Name
```

### Razorpay Dashboard Setup

1. Go to Razorpay Dashboard → Settings → Webhooks
2. Add webhook endpoint: `https://yourdomain.com/api/webhook/razorpay/payment`
3. Select events:
   - `payment.authorized`
   - `payment.captured`
   - `payment.failed`
   - `order.paid`
4. Generate and copy webhook secret
5. Add to environment variables

## UI/UX Experience

### For Counter Volunteer

**Before payment:**

```
┌──────────────────────────┐
│  Scan with UPI App       │
│                          │
│  [      QR CODE      ]   │
│  UPI QR                  │
│                          │
│  ⏳ 14:32 (Timer)        │
│  ⏳ Waiting for payment  │
│                          │
│  Point your UPI app's    │
│  camera at this QR code  │
│                          │
│  [Confirm Payment ...]   │
└──────────────────────────┘
```

**Timer updates second by second:**

```
14:32 → 14:31 → 14:30 → ... → 00:05 → 00:04 → 00:00
```

**When payment received:**

```
✨ Payment Successful!      ✅ Shows success modal
₹500 added to wallet
Redirecting...
```

**If expired (15 minutes):**

```
❌ Payment Expired
Please close and try again
```

## API Endpoints

### Frontend APIs

| Endpoint                         | Method | Purpose                     |
| -------------------------------- | ------ | --------------------------- |
| `/wallet/initiate-upi`           | POST   | Start payment order         |
| `/wallet/check-payment-status`   | GET    | Poll for payment status     |
| `/wallet/verify-upi`             | POST   | Verify payment signature    |
| `/wallet/confirm-manual-payment` | POST   | Manual payment confirmation |

### Webhook Endpoint (Backend only)

| Endpoint                    | Method | Purpose                       |
| --------------------------- | ------ | ----------------------------- |
| `/webhook/razorpay/payment` | POST   | Receive payment notifications |

## Error Handling

### Payment Expires (15 minutes)

1. Timer reaches 00:00
2. Frontend shows "Payment Expired"
3. Polling stops
4. User must close and initiate new payment

### Webhook Fails

1. Fallback: Frontend polling detects payment (slower)
2. User can click "Confirm Payment Received" button
3. Manual confirmation triggers same wallet update

### Invalid Signature

1. Webhook rejects request
2. Logs security warning
3. No transaction created
4. User must retry

## Testing Webhooks Locally

```bash
# Use ngrok to tunnel Razorpay webhook to localhost
ngrok http 8080

# Razorpay Dashboard → Webhook URL
https://xxxx-xx-xxx-xxx-xx.ngrok.io/api/webhook/razorpay/payment

# Test with Razorpay test credentials
```

## Security Considerations

1. **Signature Verification** - All webhooks signed with secret
2. **Idempotency** - Webhook may be retried, code handles duplicates
3. **Timeout** - Order expires after 15 minutes, admin can review stale orders
4. **Transaction Records** - All payments logged for audit trail
5. **Rate Limiting** - Consider adding rate limits on polling endpoint

## Comparison with Implementations

| Feature               | Direct UPI QR | Razorpay Link | Our System   |
| --------------------- | ------------- | ------------- | ------------ |
| Automatic Detection   | ❌ No         | ✅ Yes        | ✅ Yes       |
| Webhook Support       | ❌ No         | ✅ Yes        | ✅ Yes       |
| Timer                 | ❌ No         | ✅ Yes        | ✅ Yes       |
| Payment Link          | ❌ No         | ✅ Yes        | ✅ Yes       |
| UPI QR                | ✅ Yes        | ❌ No         | ✅ Yes       |
| Fallback Confirmation | ⚠️ Manual     | ❌ No         | ✅ Yes       |
| Robust                | ⚠️ Fair       | ✅ Good       | ✅ Very Good |

## Next Steps

1. **Test Webhooks** - Use Razorpay test mode with webhook endpoint
2. **Deploy** - Ensure backend is accessible for webhooks
3. **Monitor** - Check logs for webhook deliveries
4. **Optimize** - Adjust timer duration based on testing
5. **Handle Edge Cases** - Network failures, timeout scenarios

## Files Modified

### Backend

- ✅ `RazorpayWebhookController.java` - NEW webhook endpoint
- ✅ `VolunteerController.java` - Added manual confirmation endpoint
- ✅ `VolunteerService.java` - Added payment update method

### Frontend

- ✅ `recharge-wallet.tsx` - Added timer, webhook handling, expiry logic
- ✅ `volunteer-request.ts` - Added confirmManualPayment API

## Summary

This implementation provides a **production-grade, user-friendly payment experience** similar to IRCTC and major payment platforms with:

- 🚀 **Real-time detection** via Razorpay webhooks
- ⏱️ **Clear timer** showing time remaining
- 🔄 **Automatic polling** as fallback
- 🔙 **Manual confirmation** as final fallback
- 📱 **Mobile-optimized** UI
- 🔒 **Secure** webhook verification
