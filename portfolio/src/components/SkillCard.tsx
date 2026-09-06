'use client';

import React from 'react';
import { useScrollAnimation } from '@/hooks/useScrollAnimation';

interface Skill {
  _id: string;
  name: string;
  category: string;
  proficiency: string;
  icon?: string;
  image?: string;
  description?: string;
}

interface SkillCardProps {
  skill: Skill;
  onEdit?: (skill: Skill) => void;
  onDelete?: (id: string) => void;
  isAdmin?: boolean;
}

const proficiencyColors = {
  Beginner: 'bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300',
  Intermediate: 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300',
  Advanced: 'bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-300',
  Expert: 'bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-300',
};

export default function SkillCard({ skill, onEdit, onDelete, isAdmin = false }: SkillCardProps) {
  const { ref, isVisible } = useScrollAnimation();
  const [imageLoaded, setImageLoaded] = React.useState(true);

  // Capitalize first letter helper
  const capitalizeFirstLetter = (str: string) => {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1);
  };

  const proficiencyWidth = {
    Expert: 'w-full',
    Advanced: 'w-3/4',
    Intermediate: 'w-1/2',
    Beginner: 'w-1/4',
  };

  const proficiencyGradient = {
    Expert: 'from-purple-500 to-purple-600',
    Advanced: 'from-orange-500 to-orange-600',
    Intermediate: 'from-green-500 to-green-600',
    Beginner: 'from-blue-500 to-blue-600',
  };

  const handleImageError = () => {
    setImageLoaded(false);
  };

  return (
    <div
      ref={ref}
      className={`group relative overflow-hidden rounded-2xl border border-white/20 dark:border-white/10 bg-gradient-to-br from-white/80 to-white/60 dark:from-slate-800/50 dark:to-slate-900/50 backdrop-blur-xl p-6 shadow-lg transition-all duration-500 hover:shadow-2xl hover:-translate-y-2 hover:border-white/30 dark:hover:border-white/20 ${
        isVisible
          ? 'animate-fadeInScale opacity-100'
          : 'opacity-0'
      }`}
    >
      {/* Background gradient blob */}
      <div className="absolute -top-8 -right-8 w-32 h-32 bg-gradient-to-br from-orange-400/20 to-transparent rounded-full blur-3xl group-hover:scale-150 transition-transform duration-500"></div>
      
      {/* Content wrapper */}
      <div className="relative z-10 space-y-4">
        {/* Logo and Name */}
        <div className="flex items-center gap-3">
          {skill.image && imageLoaded ? (
            <img
              src={skill.image}
              alt={capitalizeFirstLetter(skill.name)}
              className="flex-shrink-0 h-14 w-14 object-contain rounded-lg group-hover:scale-110 transition-transform duration-300 p-2 bg-white dark:bg-slate-800/50"
              onError={handleImageError}
              crossOrigin="anonymous"
              loading="lazy"
            />
          ) : skill.icon ? (
            <div className="flex-shrink-0 h-14 w-14 flex items-center justify-center rounded-lg bg-gradient-to-br from-orange-400/20 to-orange-500/20 text-2xl group-hover:scale-110 transition-transform duration-300">
              {skill.icon}
            </div>
          ) : (
            <div className="flex-shrink-0 h-14 w-14 flex items-center justify-center rounded-lg bg-gradient-to-br from-slate-300 to-slate-400 dark:from-slate-600 dark:to-slate-700 text-2xl group-hover:scale-110 transition-transform duration-300">
              ⚙️
            </div>
          )}
          <h3 className="text-lg font-bold bg-gradient-to-r from-slate-900 to-slate-700 dark:from-white dark:to-slate-300 bg-clip-text text-transparent">
            {capitalizeFirstLetter(skill.name)}
          </h3>
        </div>

        {/* Category and Proficiency */}
        <div className="flex flex-wrap gap-2">
          <span className="inline-block rounded-full bg-gradient-to-r from-slate-200/60 to-slate-100/60 dark:from-slate-700/60 dark:to-slate-800/60 backdrop-blur px-3 py-1 text-xs font-semibold text-slate-700 dark:text-slate-300">
            {skill.category}
          </span>
          <span
            className={`inline-block rounded-full px-3 py-1 text-xs font-bold ${proficiencyColors[skill.proficiency as keyof typeof proficiencyColors]}`}
          >
            {skill.proficiency}
          </span>
        </div>

        {skill.description && (
          <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">{skill.description}</p>
        )}

        {/* Animated gradient progress bar */}
        <div className="space-y-2">
          <div className="flex justify-between items-center">
            <span className="text-xs font-medium text-slate-600 dark:text-slate-400">Proficiency Level</span>
            <span className="text-xs font-bold bg-gradient-to-r from-orange-500 to-orange-600 bg-clip-text text-transparent">
              {skill.proficiency}
            </span>
          </div>
          <div className="h-3 w-full overflow-hidden rounded-full bg-gradient-to-r from-slate-200 to-slate-100 dark:from-slate-700 dark:to-slate-600 backdrop-blur">
            <div
              className={`h-full rounded-full bg-gradient-to-r ${proficiencyGradient[skill.proficiency as keyof typeof proficiencyGradient]} shadow-lg transition-all duration-1000 ${proficiencyWidth[skill.proficiency as keyof typeof proficiencyWidth]}`}
            ></div>
          </div>
        </div>

        {/* Admin Actions */}
        {isAdmin && (
          <div className="flex gap-2 border-t border-white/20 dark:border-white/10 pt-4">
            <button
              onClick={() => onEdit?.(skill)}
              className="flex-1 rounded-lg bg-gradient-to-r from-amber-500 to-amber-600 px-3 py-2 text-sm font-semibold text-white transition-all duration-300 hover:shadow-lg hover:scale-105"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete?.(skill._id)}
              className="flex-1 rounded-lg bg-gradient-to-r from-red-500 to-red-600 px-3 py-2 text-sm font-semibold text-white transition-all duration-300 hover:shadow-lg hover:scale-105"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
  
