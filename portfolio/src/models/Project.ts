import mongoose from 'mongoose';

const projectSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: true,
    },
    description: {
      type: String,
      required: true,
    },
    image: {
      type: String,
    },
    technologies: [
      {
        type: String,
      },
    ],
    link: {
      type: String,
    },
    github: {
      type: String,
    },
    startDate: {
      type: Date,
      required: true,
    },
    endDate: {
      type: Date,
    },
    featured: {
      type: Boolean,
      default: false,
    },
    projectType: {
      type: String,
      enum: ['Web', 'Mobile', 'Backend'],
      default: 'Web',
    },
  },
  { timestamps: true }
);

export default mongoose.models.Project || mongoose.model('Project', projectSchema);
