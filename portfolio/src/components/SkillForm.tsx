'use client';

import React, { useState, useEffect } from 'react';

interface Skill {
  _id?: string;
  name: string;
  category: 'Frontend' | 'Backend' | 'Database' | 'DevOps' | 'Tools' | 'Programming Language' | 'Other';
  proficiency: 'Beginner' | 'Intermediate' | 'Advanced' | 'Expert';
  icon?: string;
  image?: string;
  description?: string;
}

interface SkillFormProps {
  skill?: Skill;
  onSave: (skill: Skill) => Promise<void>;
  onCancel: () => void;
}

export default function SkillForm({ skill, onSave, onCancel }: SkillFormProps) {
  const [formData, setFormData] = useState<Skill>(
    skill || {
      name: '',
      category: 'Frontend',
      proficiency: 'Intermediate',
      icon: '',
      image: '',
      description: '',
    }
  );
  const [loading, setLoading] = useState(false);

  // Update form when editing a different skill
  useEffect(() => {
    if (skill) {
      setFormData({
        name: skill.name || '',
        category: skill.category || 'Frontend',
        proficiency: skill.proficiency || 'Intermediate',
        icon: skill.icon || '',
        image: skill.image || '',
        description: skill.description || '',
      });
    } else {
      setFormData({
        name: '',
        category: 'Frontend',
        proficiency: 'Intermediate',
        icon: '',
        image: '',
        description: '',
      });
    }
  }, [skill]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    console.log(`Changing ${name} to:`, value); // Debug log
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const capitalizedName = formData.name.charAt(0).toUpperCase() + formData.name.slice(1);
      console.log('Submitting form data:', { ...formData, name: capitalizedName }); // Debug log
      await onSave({ ...formData, name: capitalizedName });
      setFormData({
        name: '',
        category: 'Frontend',
        proficiency: 'Intermediate',
        icon: '',
        image: '',
        description: '',
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-lg bg-slate-50 dark:bg-slate-900 p-6">
      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">Skill Name</label>
        <input
          type="text"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          placeholder="e.g., React, Node.js, Python"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">Category</label>
          <select
            name="category"
            value={formData.category}
            onChange={handleChange}
            className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          >
            <option value="Frontend">Frontend</option>
            <option value="Backend">Backend</option>
            <option value="Database">Database</option>
            <option value="DevOps">DevOps</option>
            <option value="Tools">Tools</option>
            <option value="Programming Language">Programming Language</option>
            <option value="Other">Other</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">Proficiency</label>
          <select
            name="proficiency"
            value={formData.proficiency}
            onChange={handleChange}
            className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          >
            <option value="Beginner">Beginner</option>
            <option value="Intermediate">Intermediate</option>
            <option value="Advanced">Advanced</option>
            <option value="Expert">Expert</option>
          </select>
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">Skill Image URL</label>
        <input
          type="text"
          name="image"
          value={formData.image || ''}
          onChange={handleChange}
          className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          placeholder="https://cdn.example.com/react.png"
        />
        <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">Get logos from: <a href="https://devicon.dev" target="_blank" rel="noopener noreferrer" className="text-orange-500 hover:text-orange-600 underline">devicon.dev</a>, <a href="https://simpleicons.org" target="_blank" rel="noopener noreferrer" className="text-orange-500 hover:text-orange-600 underline">simpleicons.org</a></p>
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">Icon Emoji (fallback if no image)</label>
        <input
          type="text"
          name="icon"
          value={formData.icon || ''}
          onChange={handleChange}
          className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          placeholder="Icon emoji or URL"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">Description (optional)</label>
        <textarea
          name="description"
          value={formData.description || ''}
          onChange={handleChange}
          className="mt-1 w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-2 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-orange-500"
          rows={3}
          placeholder="Brief description of your experience with this skill"
        />
      </div>

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={loading}
          className="flex-1 rounded-lg bg-gradient-to-r from-orange-500 to-orange-600 px-4 py-2 font-semibold text-white transition-all hover:shadow-lg disabled:opacity-50"
        >
          {loading ? 'Saving...' : 'Save Skill'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="flex-1 rounded-lg border-2 border-slate-300 dark:border-slate-600 px-4 py-2 font-semibold text-slate-700 dark:text-slate-300 transition-all hover:bg-slate-100 dark:hover:bg-slate-800"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
