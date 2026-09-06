'use client';

import React from 'react';
import { FiExternalLink, FiGithub, FiCode } from 'react-icons/fi';
import { useScrollAnimation } from '@/hooks/useScrollAnimation';

interface Project {
  _id: string;
  title: string;
  description: string;
  image?: string;
  technologies: string[];
  link?: string;
  github?: string;
  startDate: string;
  endDate?: string;
  projectType?: 'Web' | 'Mobile' | 'Backend';
}

interface ProjectCardProps {
  project: Project;
  onEdit?: (project: Project) => void;
  onDelete?: (id: string) => void;
  isAdmin?: boolean;
}

export default function ProjectCard({ project, onEdit, onDelete, isAdmin = false }: ProjectCardProps) {
  const { ref, isVisible } = useScrollAnimation();

  return (
    <div
      ref={ref}
      className={`group relative h-full overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 shadow-md dark:shadow-lg transition-all duration-300 hover:shadow-xl dark:hover:shadow-2xl hover:-translate-y-1 ${
        isVisible
          ? 'animate-fadeInScale opacity-100'
          : 'opacity-0'
      }`}
    >
      {/* Top accent gradient bar */}
      <div className="h-1 w-full bg-gradient-to-r from-blue-500 to-cyan-500"></div>

      {/* Image Section with Placeholder */}
      <div className="relative h-48 w-full overflow-hidden bg-gradient-to-br from-slate-100 to-slate-200 dark:from-slate-800 dark:to-slate-700">
        {project.image ? (
          <img
            src={project.image}
            alt={project.title}
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-110"
          />
        ) : (
          <div className="h-full w-full flex items-center justify-center bg-gradient-to-br from-blue-400/20 to-cyan-400/20 dark:from-blue-900/30 dark:to-cyan-900/30">
            <FiCode className="h-16 w-16 text-slate-400 dark:text-slate-600" />
          </div>
        )}
        {/* Overlay on hover with fade effect */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300"></div>
      </div>

      <div className="p-6 space-y-4">
        <div className="flex items-start justify-between gap-2">
          <div className="flex-1">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">{project.title}</h3>
            <p className="mt-2 line-clamp-2 text-sm text-slate-600 dark:text-slate-400">{project.description}</p>
          </div>
          {project.projectType && (
            <span className={`inline-block whitespace-nowrap rounded-lg px-3 py-1 text-xs font-semibold transition-colors ${
              project.projectType === 'Web' ? 'bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300' :
              project.projectType === 'Mobile' ? 'bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300' :
              'bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300'
            }`}>
              {project.projectType}
            </span>
          )}
        </div>

        {/* Technologies */}
        {project.technologies.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {project.technologies.slice(0, 3).map((tech) => (
              <span
                key={tech}
                className="inline-block rounded-full bg-blue-100 dark:bg-blue-900/40 px-3 py-1 text-xs font-semibold text-blue-700 dark:text-blue-300 transition-all duration-200 hover:bg-blue-200 dark:hover:bg-blue-900/60"
              >
                {tech}
              </span>
            ))}
            {project.technologies.length > 3 && (
              <span className="inline-block rounded-full bg-slate-200 dark:bg-slate-800 px-3 py-1 text-xs font-semibold text-slate-600 dark:text-slate-400">
                +{project.technologies.length - 3}
              </span>
            )}
          </div>
        )}

        {/* Action Links */}
        <div className="flex flex-wrap gap-2 pt-2">
          {project.link && (
            <a
              href={project.link}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-rose-600 dark:bg-rose-700 px-4 py-2 text-sm font-semibold text-white transition-all duration-200 hover:bg-rose-700 dark:hover:bg-rose-600 hover:shadow-lg transform hover:-translate-y-0.5 active:scale-95"
            >
              <FiExternalLink className="h-4 w-4" />
              View Live
            </a>
          )}
          {project.github && (
            <a
              href={project.github}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-700 dark:text-slate-300 transition-all duration-200 hover:bg-slate-50 dark:hover:bg-slate-700 hover:border-slate-400 dark:hover:border-slate-500 hover:shadow-md transform hover:-translate-y-0.5 active:scale-95"
            >
              <FiGithub className="h-4 w-4" />
              GitHub
            </a>
          )}
        </div>

        {isAdmin && (
          <div className="mt-4 flex gap-2 border-t border-slate-200 dark:border-slate-700 pt-4">
            <button
              onClick={() => onEdit?.(project)}
              className="flex-1 rounded-lg bg-amber-500 dark:bg-amber-600 px-3 py-2 text-sm font-semibold text-white transition-all duration-200 hover:bg-amber-600 dark:hover:bg-amber-500 hover:shadow-lg transform hover:-translate-y-0.5 active:scale-95"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete?.(project._id)}
              className="flex-1 rounded-lg bg-red-500 dark:bg-red-600 px-3 py-2 text-sm font-semibold text-white transition-all duration-200 hover:bg-red-600 dark:hover:bg-red-500 hover:shadow-lg transform hover:-translate-y-0.5 active:scale-95"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
