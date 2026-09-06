import mongoose from 'mongoose';

const experienceSchema = new mongoose.Schema(
  {
    company: {
      type: String,
      required: true,
    },
    position: {
      type: String,
      required: true,
    },
    description: {
      type: String,
      required: true,
    },
    startDate: {
      type: Date,
      required: true,
    },
    endDate: {
      type: Date,
    },
    isCurrentRole: {
      type: Boolean,
      default: false,
    },
    technologies: [
      {
        type: String,
      },
    ],
  },
  { timestamps: true }
);

export default mongoose.models.Experience || mongoose.model('Experience', experienceSchema);
