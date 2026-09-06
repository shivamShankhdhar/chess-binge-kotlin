'use client';

import React, { useState, useEffect } from 'react';

interface ExperienceFormProps {
  onSubmit: (data: any) => void;
  initialData?: any;
}

export default function ExperienceForm({ onSubmit, initialData }: ExperienceFormProps) {
  const [formData, setFormData] = useState({
    company: '',
    position: '',
    description: '',
    startDate: '',
    endDate: '',
    isCurrentRole: false,
    technologies: '',
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        company: initialData.company || '',
        position: initialData.position || '',
        description: initialData.description || '',
        startDate: initialData.startDate?.split('T')[0] || '',
        endDate: initialData.endDate?.split('T')[0] || '',
        isCurrentRole: initialData.isCurrentRole || false,
        technologies: Array.isArray(initialData.technologies) ? initialData.technologies.join(', ') : '',
      });
    }
  }, [initialData]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      ...formData,
      technologies: formData.technologies.split(',').map(t => t.trim()).filter(t => t),
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6 rounded-xl border border-slate-200 bg-white p-8 shadow-md">
      <h2 className="text-2xl font-bold text-slate-900">{initialData ? 'Edit Experience' : 'Add New Experience'}</h2>

      <div className="grid gap-6 md:grid-cols-2">
        <div>
          <label className="block text-sm font-semibold text-slate-700">Company</label>
          <input
            type="text"
            name="company"
            value={formData.company}
            onChange={handleChange}
            required
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
            placeholder="Company name"
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-slate-700">Position</label>
          <input
            type="text"
            name="position"
            value={formData.position}
            onChange={handleChange}
            required
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
            placeholder="Job title"
          />
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div>
          <label className="block text-sm font-semibold text-slate-700">Start Date</label>
          <input
            type="date"
            name="startDate"
            value={formData.startDate}
            onChange={handleChange}
            required
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-slate-700">End Date</label>
          <input
            type="date"
            name="endDate"
            value={formData.endDate}
            onChange={handleChange}
            disabled={formData.isCurrentRole}
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none disabled:bg-slate-100"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-semibold text-slate-700">Description</label>
        <textarea
          name="description"
          value={formData.description}
          onChange={handleChange}
          required
          rows={4}
          className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
          placeholder="Job description and responsibilities"
        />
      </div>

      <div>
        <label className="block text-sm font-semibold text-slate-700">Technologies (comma-separated)</label>
        <input
          type="text"
          name="technologies"
          value={formData.technologies}
          onChange={handleChange}
          className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
          placeholder="React, Node.js, PostgreSQL"
        />
      </div>

      <div className="flex items-center gap-3">
        <input
          type="checkbox"
          name="isCurrentRole"
          id="isCurrentRole"
          checked={formData.isCurrentRole}
          onChange={handleChange}
          className="h-5 w-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
        />
        <label htmlFor="isCurrentRole" className="text-sm font-semibold text-slate-700">
          Currently working here
        </label>
      </div>

      <button
        type="submit"
        className="w-full rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white transition-colors hover:bg-blue-700"
      >
        {initialData ? 'Update Experience' : 'Add Experience'}
      </button>
    </form>
  );
}
