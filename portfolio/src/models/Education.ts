import mongoose from 'mongoose';

const educationSchema = new mongoose.Schema(
  {
    school: {
      type: String,
      required: true,
    },
    degree: {
      type: String,
      required: true,
    },
    field: {
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
    description: {
      type: String,
    },
    grade: {
      type: String,
    },
  },
  { timestamps: true }
);

export default mongoose.models.Education || mongoose.model('Education', educationSchema);
