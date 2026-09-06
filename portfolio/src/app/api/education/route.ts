import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import Education from '@/models/Education';

export async function GET() {
  try {
    await connectDB();
    const education = await Education.find().sort({ startDate: -1 });
    return NextResponse.json(education);
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const body = await request.json();
    
    const education = new Education(body);
    const newEducation = await education.save();
    
    return NextResponse.json(newEducation, { status: 201 });
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
      { status: 400 }
    );
  }
}
