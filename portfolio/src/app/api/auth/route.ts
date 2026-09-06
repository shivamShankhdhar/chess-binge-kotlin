import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import { Admin } from '@/models/Admin';
import ValidAdmin from '@/models/ValidAdmin';
import { sendOTPEmail } from '@/lib/email';

// Generate OTP
function generateOTP() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const { email, action, otp } = await request.json();

    if (action === 'sendOTP') {
      // Check if email is a valid admin email
      const validAdmin = await ValidAdmin.findOne({ email: email.toLowerCase() });
      if (!validAdmin) {
        return NextResponse.json(
          { error: 'This email is not authorized to access admin panel' },
          { status: 403 }
        );
      }

      const generatedOtp = generateOTP();
      const otpExpiry = new Date(Date.now() + 10 * 60 * 1000); // 10 minutes

      let admin = await Admin.findOne({ email });
      if (!admin) {
        admin = new Admin({ email, otp: generatedOtp, otpExpiry });
      } else {
        admin.otp = generatedOtp;
        admin.otpExpiry = otpExpiry;
      }
      await admin.save();

      console.log(`Generated OTP for ${email}: ${generatedOtp} (expires at ${otpExpiry.toISOString()})`);

      // Send OTP via email
      const emailResult = await sendOTPEmail(email, generatedOtp);
      
      if (!emailResult.success) {
        return NextResponse.json(
          { error:  'Failed to send OTP email' },
          { status: 500 }
        );
      }

      return NextResponse.json({
        message: 'OTP sent to your email',
        success: true,
      });
    } else if (action === 'verifyOTP') {
      const admin = await Admin.findOne({ email });
      if (!admin) {
        return NextResponse.json(
          { error: 'Email not found' },
          { status: 404 }
        );
      }

      if (!admin.otp || admin.otp !== otp) {
        return NextResponse.json(
          { error: 'Invalid OTP' },
          { status: 400 }
        );
      }

      if (admin.otpExpiry && admin.otpExpiry < new Date()) {
        return NextResponse.json(
          { error: 'OTP has expired' },
          { status: 400 }
        );
      }

      admin.isVerified = true;
      admin.otp = undefined;
      admin.otpExpiry = undefined;
      admin.lastLogin = new Date();
      await admin.save();

      // Create session token (in production, use proper JWT)
      const token = Buffer.from(email).toString('base64');

      return NextResponse.json({
        success: true,
        token,
        email,
      });
    }

    return NextResponse.json(
      { error: 'Invalid action' },
      { status: 400 }
    );
  } catch (error) {
    console.error('Auth error:', error);
    return NextResponse.json(
      { error: 'Authentication failed' },
      { status: 500 }
    );
  }
}
