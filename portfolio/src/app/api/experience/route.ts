import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import Experience from '@/models/Experience';

export async function GET() {
  try {
    await connectDB();
    const experience = await Experience.find().sort({ startDate: -1 });
    return NextResponse.json(experience);
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
    
    const exp = new Experience(body);
    const newExp = await exp.save();
    
    return NextResponse.json(newExp, { status: 201 });
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
      { status: 400 }
    );
  }
}
