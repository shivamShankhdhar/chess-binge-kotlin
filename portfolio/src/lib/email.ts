import nodemailer from 'nodemailer';

// ✅ Create transporter (robust config)
const transporter = nodemailer.createTransport({
  host: 'smtp.gmail.com',
  port: 465,
  secure: true,
  auth: {
    user: process.env.GMAIL_USER!,
    pass: process.env.GMAIL_PASSWORD!,
  },
});

// ✅ Verify transporter once (optional but helpful)
transporter.verify((error, success) => {
  if (error) {
    console.error('❌ Mail transporter error:', error);
  } else {
    console.log('✅ Mail server is ready');
  }
});

// ================= OTP EMAIL =================
export async function sendOTPEmail(
  email: string,
  otp: string,
  name?: string
) {
  try {
    const mailOptions = {
      from: `"Shivam Portfolio" <${process.env.GMAIL_USER}>`,
      to: email,
      subject: 'Your OTP for Portfolio Admin Access',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <div style="background: linear-gradient(135deg, #FF6B35 0%, #FFA96D 100%); padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
            <h1 style="color: white; margin: 0;">Portfolio Admin</h1>
          </div>
          <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
            <p>Hello ${name || 'Admin'},</p>
            <p>You requested an OTP. Use this:</p>
            <div style="background: white; border: 2px solid #FF6B35; padding: 20px; text-align: center; border-radius: 8px;">
              <h2 style="letter-spacing: 5px; color: #FF6B35;">${otp}</h2>
            </div>
            <p style="font-size: 12px; color: gray;">Expires in 10 minutes</p>
          </div>
        </div>
      `,
    };

    await transporter.sendMail(mailOptions);

    console.log(`✅ OTP email sent to ${email}`);
    return { success: true };
  } catch (error) {
    console.error('❌ OTP email error:', error);
    return { success: false };
  }
}

// ================= CONTACT EMAIL =================
export async function sendContactConfirmationEmail(
  visitorEmail: string,
  visitorName: string,
  message: string,
  developerEmail: string,
  developerName: string
) {
  try {
    const visitorMail = {
      from: `"Shivam Portfolio" <${process.env.GMAIL_USER}>`,
      to: visitorEmail,
      subject: 'We received your message',
      html: `
        <p>Hi ${visitorName},</p>
        <p>Thanks for contacting us. We'll reply soon.</p>
        <p><strong>Your Message:</strong></p>
        <p>${message}</p>
      `,
    };

    const developerMail = {
      from: `"Portfolio Bot" <${process.env.GMAIL_USER}>`,
      to: developerEmail,
      subject: `New message from ${visitorName}`,
      html: `
        <p><strong>Name:</strong> ${visitorName}</p>
        <p><strong>Email:</strong> ${visitorEmail}</p>
        <p><strong>Message:</strong></p>
        <p>${message}</p>
      `,
    };

    await Promise.all([
      transporter.sendMail(visitorMail),
      transporter.sendMail(developerMail),
    ]);

    console.log('✅ Contact emails sent');
    return { success: true };
  } catch (error) {
    console.error('❌ Contact email error:', error);
    return { success: false };
  }
}