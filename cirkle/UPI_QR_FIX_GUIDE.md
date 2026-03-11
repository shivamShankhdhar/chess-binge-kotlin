# UPI QR Code Format Issue - Fix & Troubleshooting Guide

## Problem

UPI payment apps are rejecting the generated QR code with error: **"The selected QR is unable to recognize the valid QR code"**

## Root Cause

The UPI string format needed simplification and the merchant UPI ID may not be valid/registered.

## Changes Made

### 1. **Simplified UPI String Format** (RazorpayService.java)

**OLD FORMAT:**

```
upi://pay?pa=cirkle@okhdfcbank&pn=Cirkle&tn=Wallet%20Topup%20-%20order_xyz&am=500.00&tr=order_xyz
```

**NEW FORMAT:**

```
upi://pay?pa=cirkle@okhdfcbank&pn=Cirkle&tn=Cirkle%20Wallet&am=500.00&cu=INR&tr=ABC123
```

**Changes Made:**

- ✅ Simplified transaction note from "Wallet Topup - {orderId}" to just "Cirkle Wallet"
- ✅ Removed orderId from description (reduces length and special characters)
- ✅ Cleaned transaction reference (alphanumeric only, last 10 chars of order ID)
- ✅ Added `cu=INR` parameter (specifies currency explicitly)
- ✅ Enhanced logging to show all parameters

### 2. **Merchant UPI ID Configuration** (application.properties)

**CRITICAL ISSUE:** `cirkle@okhdfcbank` is likely NOT a valid merchant UPI ID

You need to replace it with your actual registered merchant UPI ID from your payment processor.

```properties
# Current (NEEDS REPLACEMENT)
upi.merchant.id=${UPI_MERCHANT_ID:cirkle@okhdfcbank}

# Should be (example):
upi.merchant.id=${UPI_MERCHANT_ID:yourmerchant@okaxis}
```

## Step-by-Step Fix Instructions

### Step 1: Get Valid Merchant UPI ID

Contact your payment processor and get a registered merchant UPI ID. Examples of valid formats:

- `business@okaxis` (Axis Bank)
- `merchant@okhdfcbank` (HDFC Bank)
- `shop@okicici` (ICICI Bank)
- `store@upi` (Specific providers)

### Step 2: Update Configuration

**Option A: Environment Variable (RECOMMENDED for production)**

```bash
export UPI_MERCHANT_ID="your_merchant@bankcode"
```

**Option B: Direct Property Update (for testing)**
Edit `application.properties`:

```properties
upi.merchant.id=your_merchant@bankcode
upi.merchant.name=Your Business Name
```

### Step 3: Rebuild Application

```bash
./mvnw clean compile  # Verify compilation
./mvnw clean package  # Build JAR
```

### Step 4: Test with Real UPI App

1. Start the application with updated merchant ID
2. Call the wallet recharge endpoint
3. Scan the generated QR code with a UPI app (Google Pay, PhonePe, etc.)
4. Verify that the UPI app recognizes the QR and shows payment details

## UPI String Format Specification

The standard UPI format we now use:

```
upi://pay?pa=<UPI_ID>&pn=<NAME>&tn=<NOTE>&am=<AMOUNT>&cu=<CURRENCY>&tr=<REFERENCE>
```

| Parameter | Value            | Notes                                      |
| --------- | ---------------- | ------------------------------------------ |
| `pa`      | UPI ID           | Merchant's UPI address (username@bankcode) |
| `pn`      | Merchant Name    | URL encoded, max 60 chars                  |
| `tn`      | Transaction Note | URL encoded, max 80 chars                  |
| `am`      | Amount           | Decimal format (e.g., 500.00)              |
| `cu`      | Currency         | Always "INR" for India                     |
| `tr`      | Reference ID     | Alphanumeric only, max 35 chars            |

## Validation Checklist

- [ ] Merchant UPI ID is valid and registered with payment processor
- [ ] Merchant name is correct and properly encoded
- [ ] Amount is in decimal format (500.00, not 50000 paise)
- [ ] Transaction reference uses only alphanumeric characters
- [ ] Application rebuilt with new configuration
- [ ] QR code scans successfully with Google Pay
- [ ] QR code scans successfully with PhonePe
- [ ] Payment details display correctly in UPI app
- [ ] Transaction goes through without "unrecognized QR" error

## Troubleshooting

### Issue: "Unable to recognize valid QR code"

**Causes & Solutions:**

1. ❌ Merchant UPI ID is not registered
   - ✅ Get a valid UPI ID from payment processor
2. ❌ Transaction note too long or has invalid characters
   - ✅ We've simplified to "Cirkle Wallet" - rebuild
3. ❌ QR generation includes order ID with special characters
   - ✅ We've cleaned this up - rebuild
4. ❌ Amount format is incorrect
   - ✅ Verify it's decimal (500.00) not paise (50000)

### Issue: "Merchant not found" or "Invalid UPI ID"

- Contact payment processor to verify/activate merchant UPI account
- Ensure merchant account is in active state
- Verify bank code is correct for your bank

### Issue: QR scans but payment fails

- Check merchant account has sufficient balance/limits
- Verify amount is within transaction limits
- Check if merchant account has payment collection enabled

## Backend Code Changes

[RazorpayService.java](cirkle-backend-common/src/main/java/com/razespace/cirklebackend/wallet/service/RazorpayService.java):

```java
private String generateUPIQRCode(Double amount, String orderId) {
    try {
        logger.info("🔄 Generating UPI QR code for order: {}, amount: ₹{}", orderId, amount);

        // Standard UPI format
        String transactionRef = orderId.substring(Math.max(0, orderId.length() - 10))
                                       .replaceAll("[^a-zA-Z0-9]", "");

        String payeeName = URLEncoder.encode(merchantName, StandardCharsets.UTF_8);
        String transactionNote = URLEncoder.encode("Cirkle Wallet", StandardCharsets.UTF_8);
        String amountStr = String.format("%.2f", amount);

        String upiString = String.format(
            "upi://pay?pa=%s&pn=%s&tn=%s&am=%s&cu=INR&tr=%s",
            merchantUPIId,
            payeeName,
            transactionNote,
            amountStr,
            transactionRef
        );

        logger.info("✅ UPI QR Code string generated: {}", upiString);
        return upiString;
    } catch (Exception e) {
        logger.error("❌ UPI QR code generation failed: {}", e.getMessage(), e);
    }
    return null;
}
```

## Testing the Fix

### Test 1: Verify QR String Format

Check logs for format:

```
✅ UPI QR Code string generated: upi://pay?pa=merchant@bank&pn=Name&tn=Cirkle%20Wallet&am=500.00&cu=INR&tr=ABC123
```

### Test 2: Scan with Google Pay

1. Generate QR code
2. Open Google Pay
3. "Send Money" → Scan QR
4. Should show merchant name and amount automatically

### Test 3: Scan with PhonePe

1. Generate QR code
2. Open PhonePe
3. "Pay to QR" → Scan QR
4. Should recognize and show payment details

## Summary

✅ **Code Changes Applied:**

- RazorpayService.java: Simplified UPI format generation
- application.properties: Added comments about merchant UPI ID requirement

⚠️ **Additional Action Required:**

- Replace `cirkle@okhdfcbank` with actual valid merchant UPI ID
- Rebuild application
- Test with real UPI apps

📝 **Next Steps:**

1. Get merchant UPI ID from payment processor
2. Update configuration
3. Rebuild and deploy
4. Test QR code scanning with real UPI apps
