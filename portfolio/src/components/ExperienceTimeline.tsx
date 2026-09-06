'use client';

import React from 'react';
import { FiCalendar, FiCheckCircle } from 'react-icons/fi';

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

interface ExperienceTimelineProps {
  experiences: Experience[];
}

export default function ExperienceTimeline({ experiences }: ExperienceTimelineProps) {
  const sortedExperiences = [...experiences].sort(
    (a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime()
  );

  return (
    <div className="relative px-4 py-8">
      
      {/* ✅ Timeline Line */}
      <div className="
        absolute top-0 bottom-0 w-1 border-l-2 border-dashed border-rose-400 dark:border-rose-500
        left-6 md:left-1/2 md:-translate-x-1/2
      "></div>

      <div className="space-y-16">
        {sortedExperiences.map((exp, index) => {
          const isLeft = index % 2 === 0;

          const startMonth = new Date(exp.startDate).toLocaleDateString('en-US', {
            month: 'short',
            year: 'numeric',
          });

          const endMonth = exp.endDate
            ? new Date(exp.endDate).toLocaleDateString('en-US', {
                month: 'short',
                year: 'numeric',
              })
            : 'Present';

          return (
            <div
              key={exp._id}
              className={`
                flex flex-col md:flex-row 
                ${isLeft ? 'md:flex-row' : 'md:flex-row-reverse'}
                relative
              `}
            >
              
              {/* ✅ Card */}
              <div
                className={`
                  w-full md:w-1/2 px-2 md:px-6 pl-16 md:pl-0
                  ${isLeft ? 'md:pr-12' : 'md:pl-12'}
                `}
              >
                <div className={`${isLeft ? 'md:text-right' : 'md:text-left'}`}>
                  <div className="relative overflow-hidden rounded-2xl border border-white/20 dark:border-white/10 
                    bg-gradient-to-br from-white/80 to-white/60 
                    dark:from-slate-800/50 dark:to-slate-900/50 
                    backdrop-blur-xl p-6 shadow-lg 
                    hover:shadow-2xl transition-all duration-500 hover:-translate-y-2">

                    {/* Glow */}
                    <div className="absolute -top-8 -right-8 w-40 h-40 bg-gradient-to-br from-rose-400/20 to-transparent rounded-full blur-3xl"></div>

                    {/* Header */}
                    <div className="mb-4 relative z-10">
                      <h3 className="text-xl font-bold text-slate-900 dark:text-white">
                        {exp.company}
                      </h3>
                      <p className="text-sm font-semibold text-rose-500">
                        {exp.position}
                      </p>
                    </div>

                    {/* Date + Status */}
                    <div className="mb-4 flex flex-wrap gap-2 relative z-10">
                      <span className="rounded-full bg-rose-500/10 px-3 py-1 text-xs font-semibold text-rose-500 border border-rose-500/20 flex items-center gap-2">
                        <FiCalendar /> {startMonth} - {endMonth}
                      </span>

                      {exp.isCurrentRole && (
                        <span className="rounded-full bg-green-500/10 px-3 py-1 text-xs font-semibold text-green-500 border border-green-500/20 flex items-center gap-2">
                          <FiCheckCircle /> Current
                        </span>
                      )}
                    </div>

                    {/* Description */}
                    <p className="mb-4 text-sm text-slate-600 dark:text-slate-400">
                      {exp.description}
                    </p>

                    {/* Technologies */}
                    {exp.technologies && exp.technologies.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {exp.technologies.map((tech) => (
                          <span
                            key={tech}
                            className="text-xs px-3 py-1 rounded-full bg-purple-500/10 text-purple-500 border border-purple-500/20"
                          >
                            {tech}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* ✅ Timeline Dot */}
              <div className="absolute left-6 md:left-1/2 md:-translate-x-1/2 top-6 flex items-center justify-center">
                <div className="relative flex h-12 w-12 items-center justify-center">
                  <div className="absolute h-12 w-12 rounded-full bg-rose-500/20 blur-xl"></div>
                  <div className="h-4 w-4 rounded-full bg-rose-500 border-4 border-white dark:border-slate-900 shadow-lg"></div>
                </div>
              </div>

              {/* Empty side for desktop symmetry */}
              <div className="hidden md:block md:w-1/2"></div>
            </div>
          );
        })}
      </div>
    </div>
  );
}