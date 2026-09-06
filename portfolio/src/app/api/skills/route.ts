import { NextRequest, NextResponse } from 'next/server';
import connectDB from '@/lib/db';
import { Skill } from '@/models/Skill';

// Proficiency level ranking (higher number = higher proficiency)
const proficiencyRank = {
  'Expert': 4,
  'Advanced': 3,
  'Intermediate': 2,
  'Beginner': 1,
};

export async function GET() {
  try {
    await connectDB();
    const skills = await Skill.find();
    
    // Sort by proficiency level (descending) then by name (ascending)
    skills.sort((a, b) => {
      const rankA = proficiencyRank[a.proficiency as keyof typeof proficiencyRank] || 0;
      const rankB = proficiencyRank[b.proficiency as keyof typeof proficiencyRank] || 0;
      
      // If proficiency is different, sort by proficiency (higher first)
      if (rankA !== rankB) {
        return rankB - rankA;
      }
      
      // If proficiency is the same, sort alphabetically by name
      return a.name.localeCompare(b.name);
    });
    
    return NextResponse.json(skills);
  } catch (error) {
    console.error('Error fetching skills:', error);
    return NextResponse.json({ error: 'Failed to fetch skills' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    await connectDB();
    const data = await request.json();
    console.log('Received skill data:', data); // Debug log

    // Validate that name is provided
    if (!data.name || !data.name.trim()) {
      return NextResponse.json(
        { error: 'Skill name is required' },
        { status: 400 }
      );
    }

    // Check if skill with the same name already exists (case-insensitive)
    const existingSkill = await Skill.findOne({
      name: { $regex: new RegExp(`^${data.name.trim()}$`, 'i') }
    });

    if (existingSkill) {
      return NextResponse.json(
        { error: `Skill "${data.name}" already exists. Please use a different name or update the existing skill.` },
        { status: 409 }
      );
    }

    const capitalizedName = data.name.trim().charAt(0).toUpperCase() + data.name.trim().slice(1);

    const skill = new Skill({
      ...data,
      name: capitalizedName,
      icon: data.icon || undefined,
      image: data.image || undefined,
      description: data.description || undefined,
    });
    await skill.save();

    return NextResponse.json(skill, { status: 201 });
  } catch (error: any) {
    console.error('Error creating skill:', error);
    
    // Handle MongoDB duplicate key error
    if (error.code === 11000) {
      return NextResponse.json(
        { error: `Skill "${error.keyValue?.name}" already exists. Please use a different name.` },
        { status: 409 }
      );
    }

    return NextResponse.json(
      { error: error.message || 'Failed to create skill' },
      { status: 500 }
    );
  }
}
