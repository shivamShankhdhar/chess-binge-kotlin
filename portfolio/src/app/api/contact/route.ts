import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import Profile from '@/models/Profile';
import { sendContactConfirmationEmail } from '@/lib/email';

export async function POST(request: NextRequest) {
  try {
    await connectDB();

    const { name, email, message } = await request.json();

    // Validate input
    if (!name || !email || !message) {
      return NextResponse.json(
        { error: 'Name, email, and message are required' },
        { status: 400 }
      );
    }

    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return NextResponse.json(
        { error: 'Invalid email address' },
        { status: 400 }
      );
    }

    // Get developer's email and name from Profile
    const profile = await Profile.findOne();
    const developerEmail = process.env.ADMIN_EMAIL || 'admin@example.com';
    const developerName = profile?.name || 'Shivam Shankhdhar';

    // Send confirmation emails
    const emailResult = await sendContactConfirmationEmail(
      email,
      name,
      message,
      developerEmail,
      developerName
    );

    if (!emailResult.success) {
      return NextResponse.json(
        { error:  'Failed to send emails' },
        { status: 500 }
      );
    }

    return NextResponse.json({
      success: true,
      message: 'Your message has been sent successfully. Check your email for confirmation.',
    });
  } catch (error) {
    console.error('Contact form error:', error);
    return NextResponse.json(
      { error: 'Failed to process contact form' },
      { status: 500 }
    );
  }
}
