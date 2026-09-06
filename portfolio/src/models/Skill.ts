import mongoose from 'mongoose';

const SkillSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
    unique: true,
    lowercase: true,
    trim: true,
  },
  category: {
    type: String,
    enum: ['Frontend', 'Backend', 'Database', 'DevOps', 'Tools', 'Programming Language', 'Other'],
    required: true,
  },
  proficiency: {
    type: String,
    enum: ['Beginner', 'Intermediate', 'Advanced', 'Expert'],
    required: true,
  },
  icon: String,
  image: String,
  description: String,
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

if (mongoose.models.Skill) {
  delete mongoose.models.Skill;
}

export const Skill = mongoose.model('Skill', SkillSchema);
