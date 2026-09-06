'use client';

import React from 'react';
import { FiExternalLink, FiCalendar, FiAward } from 'react-icons/fi';
import { useScrollAnimation } from '@/hooks/useScrollAnimation';

interface Certification {
  _id: string;
  title: string;
  issuer: string;
  issueDate: string;
  expiryDate?: string;
  credentialId?: string;
  credentialUrl?: string;
  image?: string;
  description?: string;
}

interface CertificationCardProps {
  certification: Certification;
  onEdit?: (certification: Certification) => void;
  onDelete?: (id: string) => void;
  isAdmin?: boolean;
}

export default function CertificationCard({
  certification,
  onEdit,
  onDelete,
  isAdmin = false,
}: CertificationCardProps) {
  const { ref, isVisible } = useScrollAnimation();
  const issueDate = new Date(certification.issueDate).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
  });

  const expiryDate = certification.expiryDate
    ? new Date(certification.expiryDate).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
      })
    : null;

  const isExpired = expiryDate && new Date(certification.expiryDate!) < new Date();

  return (
    <div ref={ref} className={`group relative overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 shadow-md transition-all duration-300 hover:shadow-xl hover:-translate-y-1 ${isVisible ? 'animate-fadeInScale opacity-100' : 'opacity-0'}`}>
      {/* Top accent bar */}
      <div className="h-1 w-full bg-gradient-to-r from-orange-500 to-orange-600"></div>

      <div className="p-6 space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">{certification.title}</h3>
            <p className="text-sm text-orange-600 dark:text-orange-400 font-semibold">{certification.issuer}</p>
          </div>
          {certification.image && (
            <img
              src={certification.image}
              alt={certification.title}
              className="h-16 w-16 rounded-lg object-cover shadow-md"
            />
          )}
        </div>

        <div className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
          <div className="flex items-center gap-2">
            <FiCalendar className="h-4 w-4" />
            <span>
              {issueDate}
              {expiryDate && (
                <>
                  {' - '}
                  <span className={isExpired ? 'line-through text-red-500' : ''}>{expiryDate}</span>
                </>
              )}
            </span>
          </div>

          {certification.credentialId && (
            <div className="flex items-center gap-2">
              <FiAward className="h-4 w-4" />
              <span>ID: {certification.credentialId}</span>
            </div>
          )}
        </div>

        {certification.description && (
          <p className="text-sm text-slate-600 dark:text-slate-400">{certification.description}</p>
        )}

        <div className="flex gap-3">
          {certification.credentialUrl && (
            <a
              href={certification.credentialUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-orange-100 dark:bg-orange-900/30 px-3 py-2 text-sm font-semibold text-orange-700 dark:text-orange-300 transition-colors hover:bg-orange-200 dark:hover:bg-orange-900/50"
            >
              View Certificate
              <FiExternalLink className="h-4 w-4" />
            </a>
          )}
          {isExpired && (
            <span className="inline-flex items-center rounded-lg bg-red-100 dark:bg-red-900/30 px-3 py-2 text-sm font-semibold text-red-700 dark:text-red-300">
              Expired
            </span>
          )}
        </div>

        {isAdmin && (
          <div className="mt-4 flex gap-2 border-t border-slate-200 dark:border-slate-700 pt-4">
            <button
              onClick={() => onEdit?.(certification)}
              className="flex-1 rounded-lg bg-amber-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-amber-600"
            >
              Edit
            </button>
            <button
              onClick={() => onDelete?.(certification._id)}
              className="flex-1 rounded-lg bg-red-500 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-red-600"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
