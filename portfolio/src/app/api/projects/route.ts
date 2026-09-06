import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import Project from '@/models/Project';

export async function GET(request: NextRequest) {
  try {
    await connectDB();
    
    // Check if requesting featured projects
    const url = new URL(request.url);
    const featured = url.searchParams.get('featured');
    
    if (featured === 'true') {
      const projects = await Project.find({ featured: true }).sort({ startDate: -1 });
      return NextResponse.json(projects);
    }
    
    const projects = await Project.find().sort({ startDate: -1 });
    return NextResponse.json(projects);
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
    
    const project = new Project(body);
    const newProject = await project.save();
    
    return NextResponse.json(newProject, { status: 201 });
  } catch (error: any) {
    return NextResponse.json(
      { message: error.message },
      { status: 400 }
    );
  }
}
