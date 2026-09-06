# Email Configuration Guide

This guide will help you set up the email features for your portfolio, including OTP authentication and contact form notifications.

## Features Implemented

✅ **OTP Email Authentication** - Secure admin login via email OTP
✅ **Contact Form Emails** - Confirmation emails to visitors + notifications to developer
✅ **Professional HTML Templates** - Beautiful, branded email designs
✅ **Error Handling** - Robust error management and user feedback

## Prerequisites

- A Gmail account
- Node.js and npm installed
- The project already has nodemailer installed

## Step 1: Get Your Gmail App Password

Since Gmail has deprecated less secure apps, you need to use an **App-Specific Password**.

### Follow these steps:

1. **Go to Google Account Security:**
   - Visit: https://myaccount.google.com/apppasswords
   - You may need to sign in and verify your identity

2. **Generate App Password:**
   - Select "Mail" from the dropdown
   - Select "Windows Computer" (or your device type)
   - Click "Generate"
   - Google will display a 16-character password
   - **Copy this password** (you'll use it shortly)

3. **Required Setup:**
   - Your Google Account must have 2-Step Verification enabled
   - If you don't have it enabled, enable it first at https://myaccount.google.com/security

## Step 2: Configure Environment Variables

Update your `.env.local` file with your Gmail credentials:

```env
# Email Configuration
GMAIL_USER=your-email@gmail.com
GMAIL_PASSWORD=your-16-character-app-password

# Admin Email (receives contact form submissions)
ADMIN_EMAIL=your-email@gmail.com

# Other existing configs...
MONGO_URI=your-mongodb-connection-string
```

### Example:
```env
GMAIL_USER=shivam@gmail.com
GMAIL_PASSWORD=abcd efgh ijkl mnop
ADMIN_EMAIL=shivam@gmail.com
MONGO_URI=mongodb+srv://...
```

## Features Overview

### 1. OTP Authentication

**How it works:**
- User enters their email on the login page
- System generates a 6-digit OTP
- **Email is sent** with the OTP (now using nodemailer!)
- User receives email and enters the OTP
- Upon verification, admin session is created

**Modified Files:**
- `src/app/api/auth/route.ts` - Now sends OTP via email

**Email Template Features:**
- Professional HTML design with gradient header
- Clear OTP display
- 10-minute expiration notice
- Security notice for users who didn't request it

### 2. Contact Form Submission

**How it works:**
1. Visitor fills out the contact form on your portfolio
2. Two emails are sent:
   - **To Visitor:** Confirmation that you received their message
   - **To Developer:** Notification with visitor's details

**New Endpoint:**
- `POST /api/contact` - Handles form submissions

**New File:**
- `src/lib/email.ts` - Email service with both functions:
  - `sendOTPEmail()` - For authentication
  - `sendContactConfirmationEmail()` - For contact form submissions

**Updated Files:**
- `src/app/page.tsx` - Contact form now uses the API instead of mailto

**Email Templates:**
- **Visitor Email:** Thank you message with their submission preview
- **Developer Email:** Full details about the visitor and their message

## Testing the Setup

### Test OTP Email:
1. Go to `/login` page
2. Enter the admin email you configured
3. Check your inbox for the OTP email
4. Enter the OTP to verify

### Test Contact Form:
1. Scroll to the "Let's Work Together" section
2. Fill out the form with test data
3. Submit and wait for confirmation
4. Check your inbox for:
   - Confirmation email (from your email)
   - Notification email (also from your email)

## Troubleshooting

### "534 Application-specific password required"
- ✅ Make sure you're using the **16-character app password**, not your regular Gmail password
- ✅ Ensure 2-Step Verification is enabled on your Google Account
- ✅ Check that `GMAIL_USER` and `GMAIL_PASSWORD` are correctly set in `.env.local`

### Emails not being received
- ✅ Check console logs for error messages
- ✅ Verify email address in `.env.local` matches your actual Gmail
- ✅ Check spam/junk folder
- ✅ Make sure MongoDB is connected (check `MONGO_URI`)

### "ENOTFOUND" or network errors
- ✅ Make sure you're connected to the internet
- ✅ Verify firewall isn't blocking Gmail SMTP
- ✅ Check that environment variables are loaded (restart dev server)

### Form shows error but no email sent
- ✅ Check browser console for error messages
- ✅ Check server logs for backend errors
- ✅ Verify all environment variables are set
- ✅ Ensure Profile document exists in MongoDB (admin should have created one)

## Advanced Configuration

### Using a Different Email Provider

If you want to use a different provider (SendGrid, AWS SES, etc.), modify `src/lib/email.ts`:

```javascript
// Example for SendGrid
import sgMail from '@sendgrid/mail';

sgMail.setApiKey(process.env.SENDGRID_API_KEY);

// Then use sgMail.send() instead of nodemailer
```

### Email Rate Limiting

To prevent spam, add rate limiting to the contact endpoint:

```javascript
// In src/app/api/contact/route.ts
const rateLimitMap = new Map();

function checkRateLimit(ip: string) {
  const now = Date.now();
  const lastRequest = rateLimitMap.get(ip) || 0;
  if (now - lastRequest < 60000) { // 1 minute limit
    return false;
  }
  rateLimitMap.set(ip, now);
  return true;
}
```

### Custom Email Templates

Update the HTML in `sendOTPEmail()` or `sendContactConfirmationEmail()` functions to match your branding.

## Email Templates Reference

### OTP Email
```
✉️ Recipient: The admin
📧 Subject: "Your OTP for Portfolio Admin Access"
📝 Contains: 6-digit OTP, expiration info, security notice
```

### Visitor Confirmation Email
```
✉️ Recipient: The person who filled the contact form
📧 Subject: "We received your message - Portfolio Contact"
📝 Contains: Thank you message, their submitted message
```

### Developer Notification Email
```
✉️ Recipient: ADMIN_EMAIL (you)
📧 Subject: "New Contact Message from [Visitor Name]"
📝 Contains: Visitor details, email, full message with reply link
```

## Security Notes

- ⚠️ **Never commit `.env.local`** - Keep credentials secure
- ✅ `.env.local` is already in `.gitignore`
- ✅ App passwords expire if not used for 30 days (Google will regenerate)
- ✅ Consider rotating credentials periodically
- ✅ OTP expires after 10 minutes
- ✅ All emails are HTML-based with no credentials exposed

## Production Deployment

When deploying to production (e.g., Vercel):

1. **Set environment variables in platform:**
   - Go to your platform's environment settings
   - Add `GMAIL_USER`, `GMAIL_PASSWORD`, `ADMIN_EMAIL`

2. **Consider:**
   - Using a dedicated email service account (not personal)
   - Setting up email domain verification (DKIM/SPF) to improve deliverability
   - Implementing email templates in a database for easier updates
   - Using a service like SendGrid for better tracking and analytics

## Support & Documentation

- [Nodemailer Documentation](https://nodemailer.com/)
- [Gmail App Passwords Help](https://support.google.com/accounts/answer/185833)
- [Next.js API Routes](https://nextjs.org/docs/api-routes/introduction)

## Files Modified/Created

```
src/lib/email.ts                    ← New: Email service
src/app/api/contact/route.ts       ← New: Contact form endpoint
src/app/api/auth/route.ts          ← Modified: Now uses sendOTPEmail()
src/app/page.tsx                    ← Modified: Contact form uses API
.env.local                          ← Modified: Added email config
.env.example                        ← Updated: Email config template
```

---

**Ready to test?** Start your dev server and try sending an email! 🚀
