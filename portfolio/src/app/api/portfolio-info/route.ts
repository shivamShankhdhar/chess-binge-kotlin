import connectDB from '@/lib/db';
import ValidAdmin from '@/models/ValidAdmin';

type AdminType = {
  _id: string;
  email: string;
  name?: string;
};

export async function GET() {
  try {
    await connectDB();

    const admin = await ValidAdmin.findOne().lean<AdminType>();

    if (!admin) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'No admin email found',
        }),
        { status: 404 }
      );
    }

    return new Response(
      JSON.stringify({
        success: true,
        email: admin.email,
        name: admin.name,
      }),
      { status: 200 }
    );
  } catch (error) {
    console.error('Portfolio info error:', error);
    return new Response(
      JSON.stringify({
        success: false,
        error: 'Failed to fetch portfolio info',
      }),
      { status: 500 }
    );
  }
}