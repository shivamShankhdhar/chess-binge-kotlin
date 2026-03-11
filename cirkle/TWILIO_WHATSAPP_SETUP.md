# Twilio WhatsApp Configuration Guide

## Current Status

Your backend is configured to send wallet QR codes via WhatsApp using Twilio. However, the WhatsApp channel needs to be properly set up in your Twilio account.

## Error: "Twilio could not find a Channel with the specified From address"

This error occurs when:

1. The WhatsApp number in `application.properties` is not activated
2. The WhatsApp sandbox is not configured
3. The channel doesn't exist in your Twilio account

## Current Configuration

**File:** `cirkle-backend-common/src/main/resources/application.properties`

```properties
twilio.whatsapp-number=whatsapp:+14155552671
```

This is Twilio's **sandbox number** for testing.

## Setup Instructions

### Option 1: Development/Testing (Using Twilio Sandbox)

1. **Login to Twilio Console**: https://console.twilio.com
2. **Navigate to**:
   - Left menu → Messaging → WhatsApp Sandbox
3. **Join the Sandbox**:
   - You'll see a QR code and/or a sandbox join code
   - Text the code to the sandbox number shown (usually `+1-415-523-8886`)
   - Or scan the QR code with WhatsApp
4. **After joining**:
   - Your phone number is registered as a test user
   - You can receive test messages from the sandbox
5. **Keep the current config**:
   ```properties
   twilio.whatsapp-number=whatsapp:+14155552671
   ```

### Option 2: Production (WhatsApp Business Account)

For production, you need a WhatsApp Business Account:

1. **Go to Twilio Console** → Messaging → WhatsApp
2. **Request a WhatsApp Business Account**:
   - Follow Twilio's verification process
   - This requires:
     - Business information
     - Phone number verification
     - WhatsApp Business Account creation
3. **Once approved, get your WhatsApp Number**:
   - Format: `whatsapp:+91XXXXXXXXXX` (for India)
4. **Update `application.properties`**:
   ```properties
   twilio.whatsapp-number=whatsapp:+91XXXXXXXXXX
   ```
5. **Rebuild and redeploy** the backend

## Quick Action Plan

### Immediate Fix (Testing)

```bash
# 1. Login to Twilio Console
# 2. Go to: Messaging > WhatsApp Sandbox
# 3. Join the sandbox with your phone number
# 4. No code changes needed - use current config
# 5. Test sending QR code from your app
```

### Verify Configuration

**Check what's configured:**

```bash
# In application.properties
twilio.account-sid=YOUR_TWILIO_ACCOUNT_SID
twilio.auth-token=YOUR_TWILIO_AUTH_TOKEN
twilio.whatsapp-number=whatsapp:+14155552671
```

**Test from backend:**

```java
// The backend now has automatic SMS fallback
// If WhatsApp fails, it will try SMS to send the QR link
// You'll see logs indicating which method was used
```

## Message Format

When the QR is sent successfully, users receive:

**Via WhatsApp:**

- Text message with wallet details (formatted with emojis and markdown)
- Attached QR code image (500x500px)
- Balance, event name, status, and usage instructions

**Via SMS (Fallback):**

- Text message with QR code URL and balance overview
- If WhatsApp sandbox not joined

## Troubleshooting

| Problem                       | Solution                                            |
| ----------------------------- | --------------------------------------------------- |
| "Channel not found" error     | Join WhatsApp sandbox first (see Option 1 above)    |
| Message sent but not received | Verify phone number is joined to sandbox            |
| QR image not displaying       | Verify S3 bucket permissions (`cirkle-images-prod`) |
| Wrong balance shown           | Check wallet.getBalance() returns correct value     |

## Testing Checklist

- [ ] Twilio account has WhatsApp enabled
- [ ] Phone number joined to WhatsApp sandbox (if testing)
- [ ] application.properties has correct `twilio.whatsapp-number`
- [ ] AWS S3 credentials configured for QR upload
- [ ] Backend running with correct configuration
- [ ] Test QR send from wallet modal in app
- [ ] Verify WhatsApp message received with QR image

## Implementation Details

The backend has been enhanced with:

1. **Better Error Handling**:
   - Catches specific Twilio API errors
   - Logs Twilio error codes and messages
   - Provides troubleshooting hints

2. **Automatic Fallback**:
   - If WhatsApp fails, automatically sends QR via SMS
   - Response includes which method was used
   - User receives QR code either way

3. **Response Structure**:
   ```json
   {
     "success": true,
     "message": "Wallet QR code image sent to WhatsApp successfully",
     "walletId": "...",
     "phoneNumber": "+919876543210",
     "qrImageUrl": "https://cirkle-images-prod.s3.ap-south-1.amazonaws.com/qr-codes/...",
     "deliveryMethod": "WHATSAPP",
     "sentAt": "2024-01-15T10:30:00"
   }
   ```

## Next Steps

1. **Join WhatsApp Sandbox** (if testing):
   - Use the steps in Option 1 above
   - Takes ~2 minutes

2. **Redeploy Backend**:
   - The updated code has better error handling
   - No property changes needed for testing

3. **Test End-to-End**:
   - Open wallet modal in app
   - Click "Send on WhatsApp"
   - Should receive message with QR image

4. **Monitor Logs**:
   - Watch backend logs for success/failure messages
   - Check which delivery method was used

## Support

For Twilio issues:

- https://support.twilio.com/hc/en-us
- https://www.twilio.com/docs/whatsapp

For Cirkle-specific issues:

- Check backend logs for detailed error messages
- Verify all credentials in application.properties
- Ensure AWS S3 bucket and permissions are correct
