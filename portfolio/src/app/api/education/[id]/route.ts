import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import Education from '@/models/Education';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    await connectDB();
    const { id } = await params;
    
    const education = await Education.findById(id);
    if (!education) {
      return NextResponse.json(
        { message: 'Education not found' },
        { status: 404 }
      );
    }
    
    return NextResponse.json(education);
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
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
    const body = await request.json();
    
    const education = await Education.findById(id);
    if (!education) {
      return NextResponse.json(
        { message: 'Education not found' },
        { status: 404 }
      );
    }
    
    Object.assign(education, body);
    const updatedEducation = await education.save();
    
    return NextResponse.json(updatedEducation);
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
      { status: 400 }
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
    
    const education = await Education.findByIdAndDelete(id);
    if (!education) {
      return NextResponse.json(
        { message: 'Education not found' },
        { status: 404 }
      );
    }
    
    return NextResponse.json({ message: 'Education deleted' });
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
      { status: 500 }
    );
  }
}
