import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import ValidAdmin from '@/models/ValidAdmin';

export async function GET(request: NextRequest) {
  try {
    await connectDB();

    const validAdmins = await ValidAdmin.find({}).select('-__v');

    return NextResponse.json({
      success: true,
      data: validAdmins,
    });
  } catch (error) {
    console.error('Error fetching valid admins:', error);
    return NextResponse.json(
      { error: 'Failed to fetch valid admins' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const { email, name, description } = await request.json();

    // Validate email format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return NextResponse.json(
        { error: 'Invalid email format' },
        { status: 400 }
      );
    }

    // Check if email already exists
    const existingAdmin = await ValidAdmin.findOne({ email: email.toLowerCase() });
    if (existingAdmin) {
      return NextResponse.json(
        { error: 'This email is already registered as an admin' },
        { status: 409 }
      );
    }

    const validAdmin = new ValidAdmin({
      email: email.toLowerCase(),
      name: name || '',
      description: description || '',
    });

    await validAdmin.save();

    return NextResponse.json({
      success: true,
      message: 'Admin email added successfully',
      data: validAdmin,
    });
  } catch (error) {
    console.error('Error adding valid admin:', error);
    return NextResponse.json(
      { error: 'Failed to add admin email' },
      { status: 500 }
    );
  }
}
