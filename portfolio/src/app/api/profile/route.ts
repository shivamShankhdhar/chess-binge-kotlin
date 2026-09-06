import connectDB from '@/lib/db';
import Profile from '@/models/Profile';

export async function GET() {
  try {
    await connectDB();

    // Get the profile (should be only one)
    const profile = await Profile.findOne().lean();

    if (!profile) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'No profile found',
        }),
        { status: 404 }
      );
    }

    return new Response(
      JSON.stringify({
        success: true,
        data: profile,
      }),
      { status: 200 }
    );
  } catch (error) {
    console.error('Profile fetch error:', error);
    return new Response(
      JSON.stringify({
        success: false,
        error: 'Failed to fetch profile',
      }),
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  try {
    await connectDB();

    const body = await request.json();
    const { name, bio, linkedinUrl, githubUrl, email, available, roles } = body;

    if (!name) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Name is required',
        }),
        { status: 400 }
      );
    }

    // Create new profile (delete existing first to ensure only one)
    await Profile.deleteMany({});
    const profile = new Profile({
      name,
      bio,
      linkedinUrl,
      githubUrl,
      email,
      available,
      roles,
    });

    await profile.save();

    return new Response(
      JSON.stringify({
        success: true,
        data: profile,
      }),
      { status: 201 }
    );
  } catch (error) {
    console.error('Profile creation error:', error);
    return new Response(
      JSON.stringify({
        success: false,
        error: 'Failed to create profile',
      }),
      { status: 500 }
    );
  }
}

export async function PUT(request: Request) {
  try {
    await connectDB();

    const body = await request.json();
    const { name, bio, linkedinUrl, githubUrl, email, available, roles } = body;

    if (!name) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Name is required',
        }),
        { status: 400 }
      );
    }

    // Update existing profile or create if doesn't exist
    const profile = await Profile.findOneAndUpdate(
      {},
      {
        name,
        bio,
        linkedinUrl,
        githubUrl,
        email,
        available,
        roles,
      },
      { upsert: true, new: true }
    );

    return new Response(
      JSON.stringify({
        success: true,
        data: profile,
      }),
      { status: 200 }
    );
  } catch (error) {
    console.error('Profile update error:', error);
    return new Response(
      JSON.stringify({
        success: false,
        error: 'Failed to update profile',
      }),
      { status: 500 }
    );
  }
}
