import mongoose from 'mongoose';

const CertificationSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true,
  },
  issuer: {
    type: String,
    required: true,
  },
  issueDate: {
    type: Date,
    required: true,
  },
  expiryDate: Date,
  credentialId: String,
  credentialUrl: String,
  image: String,
  description: String,
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

export const Certification = mongoose.models.Certification || mongoose.model('Certification', CertificationSchema);
