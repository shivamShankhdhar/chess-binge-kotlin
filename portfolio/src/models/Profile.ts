import mongoose from 'mongoose';

const profileSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: true,
      trim: true,
    },
    bio: {
      type: String,
      trim: true,
      default: '',
    },
    linkedinUrl: {
      type: String,
      trim: true,
      default: '',
    },
    githubUrl: {
      type: String,
      trim: true,
      default: '',
    },
    email: {
      type: String,
      trim: true,
      default: '',
    },
    available: {
      type: Boolean,
      default: true,
    },
    roles: {
      type: [String],
      default: [
        'Full Stack Developer',
        'Java Developer',
        'MERN Stack Developer',
        'React Native Developer',
      ],
    },
  },
  { timestamps: true }
);

export default mongoose.models.Profile || mongoose.model('Profile', profileSchema, 'profile');
