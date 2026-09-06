import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import { Certification } from '@/models/Certification';

export async function GET() {
  try {
    await connectDB();
    const certifications = await Certification.find().sort({ issueDate: -1 });
    return NextResponse.json(certifications);
  } catch (error) {
    console.error('Error fetching certifications:', error);
    return NextResponse.json({ error: 'Failed to fetch certifications' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const data = await request.json();
    
    const certification = new Certification(data);
    await certification.save();
    
    return NextResponse.json(certification, { status: 201 });
  } catch (error) {
    console.error('Error creating certification:', error);
    return NextResponse.json({ error: 'Failed to create certification' }, { status: 500 });
  }
}
