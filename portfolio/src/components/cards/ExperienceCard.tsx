'use client';

import React from 'react';
import { useScrollAnimation } from '@/hooks/useScrollAnimation';

interface Experience {
  _id: string;
  company: string;
  position: string;
  description: string;
  startDate: string;
  endDate?: string;
  isCurrentRole: boolean;
  technologies?: string[];
}

interface ExperienceCardProps {
  experience: Experience;
  onEdit?: (experience: Experience) => void;
  onDelete?: (id: string) => void;
  isAdmin?: boolean;
}

export default function ExperienceCard({ experience, onEdit, onDelete, isAdmin = false }: ExperienceCardProps) {
  const { ref, isVisible } = useScrollAnimation();
  const startMonth = new Date(experience.startDate).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
  const endMonth = experience.endDate ? new Date(experience.endDate).toLocaleDateString('en-US', { month: 'short', year: 'numeric' }) : 'Present';

  return (
    <div ref={ref} className={`group relative overflow-hidden rounded-xl border border-slate-200 bg-white p-6 shadow-md transition-all duration-300 hover:shadow-lg hover:-translate-y-1 ${isVisible ? 'animate-fadeInScale opacity-100' : 'opacity-0'}`}>
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h3 className="text-lg font-bold text-slate-900">{experience.company}</h3>
          <p className="text-sm font-semibold text-blue-600">{experience.position}</p>
        </div>
        <div className="text-right">
          <span className="rounded-full bg-green-100 px-3 py-1 text-xs font-semibold text-green-800">
            {startMonth} - {endMonth}
          </span>
          {experience.isCurrentRole && (
            <div className="mt-2 inline-block rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-800">
              Current
            </div>
          )}
        </div>
      </div>

      <p className="mb-4 text-sm text-slate-600">{experience.description}</p>

      {experience.technologies && experience.technologies.length > 0 && (
        <div className="mb-4 flex flex-wrap gap-2">
          {experience.technologies.map((tech) => (
            <span key={tech} className="inline-block rounded-full bg-purple-100 px-3 py-1 text-xs font-semibold text-purple-800">
              {tech}
            </span>
          ))}
        </div>
      )}

      {isAdmin && (
        <div className="mt-4 flex gap-2 border-t border-slate-200 pt-4">
          <button
            onClick={() => onEdit?.(experience)}
            className="flex-1 rounded-lg bg-amber-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-amber-600"
          >
            Edit
          </button>
          <button
            onClick={() => onDelete?.(experience._id)}
            className="flex-1 rounded-lg bg-red-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-600"
          >
            Delete
          </button>
        </div>
      )}
    </div>
  );
}
