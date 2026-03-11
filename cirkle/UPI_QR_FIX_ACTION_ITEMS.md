# UPI QR Code Fix - Action Items

## 🔴 CRITICAL: This is blocking your UPI QR code recognition

Your UPI payment apps are rejecting the QR code because:

1. **Merchant UPI ID is likely invalid/not registered**
   - Current: `cirkle@okhdfcbank`
   - This needs to be a real, registered merchant UPI ID
2. **UPI string format needed simplification**
   - OLD: Description included full order ID → `Wallet Topup - ORDER_XYZ123`
   - NEW: Simplified to just `Cirkle Wallet`
   - Reason: Special characters and long strings confuse QR parsers

## ✅ What I Fixed

### Backend Changes (RazorpayService.java)

```diff
- Description: "Wallet Topup - " + orderId  (LONG, SPECIAL CHARS)
+ Description: "Cirkle Wallet"               (SHORT, SIMPLE)

- Transaction Ref: Last 12 chars of orderId (may have special chars)
+ Transaction Ref: Last 10 chars, alphanumeric only

+ Added currency parameter cu=INR to UPI string
+ Enhanced logging to show all UPI parameters
```

### Configuration (application.properties)

```diff
+ Added comments explaining merchant UPI ID requirement
+ Documented format: username@bankcode
```

## 📋 What You Need to Do

### Step 1: Get Valid Merchant UPI ID (URGENT)

Contact your payment processor:

- ❓ Ask: "Do we have a registered merchant UPI ID?"
- 📧 Examples of valid formats:
  - `businessname@okaxis`
  - `merchantid@okhdfcbank`
  - `shop@upi`

### Step 2: Update Configuration

Once you have the merchant UPI ID:

**Option A: Development (quick test)**
Edit: `cirkle-backend-common/src/main/resources/application.properties`

```properties
upi.merchant.id=your_actual_merchant@bankcode
upi.merchant.name=Your Business Name
```

**Option B: Production (via environment)**

```bash
export UPI_MERCHANT_ID="your_actual_merchant@bankcode"
export UPI_MERCHANT_NAME="Your Business Name"
```

### Step 3: Rebuild & Restart

```bash
cd cirkle-backend-common
./mvnw clean package
java -jar target/cirkle-backend-0.0.1-SNAPSHOT.jar
```

### Step 4: Test

1. Call wallet recharge endpoint
2. Scan generated QR with real UPI app (Google Pay, PhonePe)
3. Verify: UPI app recognizes it (no "unrecognized QR" error)

## 🧪 Quick Validation

After rebuilding, check the logs for:

```
✅ UPI QR Code string generated successfully: upi://pay?pa=YOUR_ID&pn=Name&tn=Cirkle%20Wallet&am=500.00&cu=INR&tr=ABC123
   UPI Details: pa=YOUR_ID, pn=Name, tn=Cirkle Wallet, am=500.00, tr=ABC123
```

## 📝 Example of Working UPI String

```
upi://pay?pa=merchant123@okaxis&pn=Cirkle&tn=Cirkle%20Wallet&am=500.00&cu=INR&tr=ORDER123
```

When scanned with Google Pay or PhonePe, it should:

- ✅ Recognize the QR code
- ✅ Show "Cirkle" as payee
- ✅ Show ₹500.00 as amount
- ✅ Allow payment

## 🐛 If Still Not Working After Changes

1. **Verify merchant UPI is active**
   - Contact payment processor
   - Check if merchant account is enabled for payments

2. **Check amount format**
   - Must be decimal: 500.00 (not 50000 in paise)

3. **Test with known UPI**
   - Ask payment processor for a test UPI ID
   - Use that to verify QR generation works

4. **Enable debug logging**
   - Check backend logs for exact UPI string being generated
   - Paste that string to verify format

## 📞 Payment Processor Support

Common payment processors and their support:

- **Razorpay**: Support portal, check merchant dashboard
- **PayU**: Account settings → UPI Details
- **Instamojo**: Merchant account → Payment Methods
- **Local processor**: Check with your finance team

**Info to gather:**

- Merchant UPI ID (the one registered)
- Bank code for that UPI
- Whether account is enabled for QR code collections
- Any test UPI IDs available for testing

## Summary

✅ **DONE**: Simplified UPI string format in backend
⏳ **YOU**: Get merchant UPI ID from payment processor
⏳ **YOU**: Update configuration with real UPI ID
⏳ **YOU**: Rebuild and test with real UPI app

Current blocker: Missing valid merchant UPI ID
