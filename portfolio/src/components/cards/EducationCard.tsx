'use client';

import React from 'react';
import { useScrollAnimation } from '@/hooks/useScrollAnimation';
import { FaGraduationCap } from 'react-icons/fa';
import { FiCalendar } from 'react-icons/fi';
import { MdEdit, MdDelete } from 'react-icons/md';

interface Education {
  _id: string;
  school: string;
  degree: string;
  field: string;
  startDate: string;
  endDate?: string;
  description?: string;
  grade?: string;
}

interface EducationCardProps {
  education: Education;
  onEdit?: (education: Education) => void;
  onDelete?: (id: string) => void;
  isAdmin?: boolean;
}

export default function EducationCard({
  education,
  onEdit,
  onDelete,
  isAdmin = false,
}: EducationCardProps) {
  const { ref, isVisible } = useScrollAnimation();

  const startYear = new Date(education.startDate).getFullYear();
  const endYear = education.endDate
    ? new Date(education.endDate).getFullYear()
    : 'Present';

  return (
    <div
      ref={ref}
      className={`group relative overflow-hidden rounded-2xl border border-white/10 
      bg-white/5 backdrop-blur-xl p-6 shadow-lg 
      transition-all duration-500 hover:-translate-y-2 hover:shadow-2xl 
      hover:border-rose-500/40
      ${isVisible ? 'animate-fadeInScale opacity-100' : 'opacity-0'}`}
    >
      {/* 🔴 Glow effect */}
      <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition duration-500">
        <div className="absolute -top-10 -right-10 w-40 h-40 bg-gradient-to-br from-rose-500 to-red-600 blur-3xl opacity-30"></div>
      </div>

      {/* Top Section */}
  <div className="flex gap-5 relative z-10">

  {/* LEFT COLUMN (ICON - FIXED) */}
  <div className="flex flex-col items-center">
    <div
      className="flex h-12 w-12 items-center justify-center rounded-xl 
      bg-gradient-to-br from-rose-500 to-red-600 text-white shadow-md"
    >
      <FaGraduationCap className="text-lg" />
    </div>

    {/* Optional vertical line (timeline feel) */}
    {/* <div className="w-[2px] flex-1 bg-white/10 mt-3 rounded-full"></div> */}
  </div>

  {/* RIGHT COLUMN (CONTENT) */}
  <div className="flex flex-col gap-2 w-full">

    {/* Title */}
    <h3 className="text-lg font-semibold text-white">
      {education.school}
    </h3>

    {/* Degree */}
    <p className="text-sm text-rose-400 font-medium">
      {education.degree}
    </p>

    {/* Field */}
    <p className="text-sm text-slate-400">
      {education.field}
    </p>

    {/* Year */}
    {/* <div className="flex items-center gap-2 text-xs text-slate-400 mt-1">
      <FiCalendar />
      <span>
        {startYear} - {endYear}
      </span>
    </div> */}

    {/* Description */}
    {education.description && (
      <p className="mt-2 text-sm text-slate-400 leading-relaxed">
        {education.description}
      </p>
    )}

    {/* Grade */}
    {/* {education.grade && (
      <div
        className="mt-2 inline-block w-fit rounded-md 
        bg-rose-500/10 border border-rose-500/20 
        px-3 py-1 text-xs font-semibold text-rose-400"
      >
        GPA: {education.grade}
      </div>
    )} */}
  </div>
</div>
      {/* Admin Actions */}
      {isAdmin && (
        <div className="mt-5 flex gap-3 border-t border-white/10 pt-4 relative z-10">
          <button
            onClick={() => onEdit?.(education)}
            className="flex flex-1 items-center justify-center gap-2 
            rounded-lg bg-amber-500/90 px-3 py-2 text-sm font-semibold text-white 
            hover:bg-amber-500 transition-all"
          >
            <MdEdit />
            Edit
          </button>

          <button
            onClick={() => onDelete?.(education._id)}
            className="flex flex-1 items-center justify-center gap-2 
            rounded-lg bg-red-500/90 px-3 py-2 text-sm font-semibold text-white 
            hover:bg-red-500 transition-all"
          >
            <MdDelete />
            Delete
          </button>
        </div>
      )}
    </div>
  );
}