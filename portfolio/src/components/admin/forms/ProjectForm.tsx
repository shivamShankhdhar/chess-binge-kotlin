'use client';

import React, { useState, useEffect } from 'react';

interface ProjectFormProps {
  onSubmit: (data: any) => void;
  initialData?: any;
}

export default function ProjectForm({ onSubmit, initialData }: ProjectFormProps) {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    image: '',
    technologies: '',
    link: '',
    github: '',
    startDate: '',
    endDate: '',
    featured: false,
    projectType: 'Web',
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        title: initialData.title || '',
        description: initialData.description || '',
        image: initialData.image || '',
        technologies: Array.isArray(initialData.technologies) ? initialData.technologies.join(', ') : '',
        link: initialData.link || '',
        github: initialData.github || '',
        startDate: initialData.startDate?.split('T')[0] || '',
        endDate: initialData.endDate?.split('T')[0] || '',
        featured: initialData.featured || false,
        projectType: initialData.projectType || 'Web',
      });
    }
  }, [initialData]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
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
      <h2 className="text-2xl font-bold text-slate-900">{initialData ? 'Edit Project' : 'Add New Project'}</h2>

      <div className="grid gap-6 md:grid-cols-2">
        <div>
          <label className="block text-sm font-semibold text-slate-700">Title</label>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            required
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
            placeholder="Project title"
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-slate-700">Project Type</label>
          <select
            name="projectType"
            value={formData.projectType}
            onChange={handleChange}
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
          >
            <option value="Web">Web Application</option>
            <option value="Mobile">Mobile Application</option>
            <option value="Backend">Backend/Server</option>
          </select>
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
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
          />
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div>
          <label className="block text-sm font-semibold text-slate-700">Image URL</label>
          <input
            type="text"
            name="image"
            value={formData.image}
            onChange={handleChange}
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
            placeholder="https://example.com/image.jpg"
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
          placeholder="Project description"
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
          placeholder="React, Node.js, MongoDB"
        />
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div>
          <label className="block text-sm font-semibold text-slate-700">Project Link</label>
          <input
            type="text"
            name="link"
            value={formData.link}
            onChange={handleChange}
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
            placeholder="https://example.com"
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-slate-700">GitHub Link</label>
          <input
            type="text"
            name="github"
            value={formData.github}
            onChange={handleChange}
            className="mt-2 w-full rounded-lg border border-slate-300 px-4 py-2 text-slate-900 focus:border-blue-500 focus:outline-none"
            placeholder="https://github.com/user/repo"
          />
        </div>
      </div>

      <div className="flex items-center gap-3">
        <input
          type="checkbox"
          name="featured"
          id="featured"
          checked={formData.featured}
          onChange={handleChange}
          className="h-5 w-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
        />
        <label htmlFor="featured" className="text-sm font-semibold text-slate-700">
          Featured Project
        </label>
      </div>

      <button
        type="submit"
        className="w-full rounded-lg bg-blue-600 px-6 py-3 font-semibold text-white transition-colors hover:bg-blue-700"
      >
        {initialData ? 'Update Project' : 'Create Project'}
      </button>
    </form>
  );
}
