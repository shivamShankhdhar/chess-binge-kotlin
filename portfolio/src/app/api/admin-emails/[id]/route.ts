import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import ValidAdmin from '@/models/ValidAdmin';
import { Types } from 'mongoose';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    await connectDB();
    const { id } = await params;

    if (!Types.ObjectId.isValid(id)) {
      return NextResponse.json(
        { error: 'Invalid admin ID' },
        { status: 400 }
      );
    }

    const validAdmin = await ValidAdmin.findById(id);

    if (!validAdmin) {
      return NextResponse.json(
        { error: 'Admin email not found' },
        { status: 404 }
      );
    }

    return NextResponse.json({
      success: true,
      data: validAdmin,
    });
  } catch (error) {
    console.error('Error fetching admin email:', error);
    return NextResponse.json(
      { error: 'Failed to fetch admin email' },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    await connectDB();
    const { id } = await params;
    const { name, description } = await request.json();

    if (!Types.ObjectId.isValid(id)) {
      return NextResponse.json(
        { error: 'Invalid admin ID' },
        { status: 400 }
      );
    }

    const validAdmin = await ValidAdmin.findByIdAndUpdate(
      id,
      { name, description },
      { new: true, runValidators: true }
    );

    if (!validAdmin) {
      return NextResponse.json(
        { error: 'Admin email not found' },
        { status: 404 }
      );
    }

    return NextResponse.json({
      success: true,
      message: 'Admin email updated successfully',
      data: validAdmin,
    });
  } catch (error) {
    console.error('Error updating admin email:', error);
    return NextResponse.json(
      { error: 'Failed to update admin email' },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    await connectDB();
    const { id } = await params;

    if (!Types.ObjectId.isValid(id)) {
      return NextResponse.json(
        { error: 'Invalid admin ID' },
        { status: 400 }
      );
    }

    const validAdmin = await ValidAdmin.findByIdAndDelete(id);

    if (!validAdmin) {
      return NextResponse.json(
        { error: 'Admin email not found' },
        { status: 404 }
      );
    }

    return NextResponse.json({
      success: true,
      message: 'Admin email deleted successfully',
    });
  } catch (error) {
    console.error('Error deleting admin email:', error);
    return NextResponse.json(
      { error: 'Failed to delete admin email' },
      { status: 500 }
    );
  }
}
