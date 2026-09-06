'use client';

import { FormEvent, useState, useEffect } from 'react';

interface ProfileFormProps {
  onSuccess?: () => void;
  token: string;
}

export default function ProfileForm({ onSuccess, token }: ProfileFormProps) {
  const [formData, setFormData] = useState({
    name: '',
    bio: '',
    linkedinUrl: '',
    githubUrl: '',
    email: '',
    available: true,
    roles: [] as string[],
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await fetch('/api/profile');
        if (res.ok) {
          const data = await res.json();
          setFormData(data.data);
        }
      } catch (error) {
        console.error('Error fetching profile:', error);
      }
    };

    fetchProfile();
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleAvailableChange = (checked: boolean) => {
    setFormData((prev) => ({
      ...prev,
      available: checked,
    }));
  };

  const handleRolesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rolesText = e.target.value;
    const rolesArray = rolesText
      .split(',')
      .map(role => role.trim())
      .filter(role => role.length > 0);
    setFormData((prev) => ({
      ...prev,
      roles: rolesArray,
    }));
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      const res = await fetch('/api/profile', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      const data = await res.json();

      if (res.ok) {
        setMessage('Profile updated successfully!');
        onSuccess?.();
      } else {
        setMessage(data.error || 'Failed to update profile');
      }
    } catch (error) {
      console.error('Error updating profile:', error);
      setMessage('Error updating profile');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <label htmlFor="name" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
          Name *
        </label>
        <input
          type="text"
          id="name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
          className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-orange-500 focus:outline-none dark:focus:border-orange-400"
          placeholder="Your Name"
        />
      </div>

      <div>
        <label htmlFor="bio" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
          Bio
        </label>
        <textarea
          id="bio"
          name="bio"
          value={formData.bio}
          onChange={handleChange}
          rows={4}
          className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-orange-500 focus:outline-none dark:focus:border-orange-400 resize-none"
          placeholder="Your bio..."
        ></textarea>
      </div>

      <div>
        <label htmlFor="linkedinUrl" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
          LinkedIn URL
        </label>
        <input
          type="url"
          id="linkedinUrl"
          name="linkedinUrl"
          value={formData.linkedinUrl}
          onChange={handleChange}
          className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-orange-500 focus:outline-none dark:focus:border-orange-400"
          placeholder="https://linkedin.com/in/username"
        />
      </div>

      <div>
        <label htmlFor="githubUrl" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
          GitHub URL
        </label>
        <input
          type="url"
          id="githubUrl"
          name="githubUrl"
          value={formData.githubUrl}
          onChange={handleChange}
          className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-orange-500 focus:outline-none dark:focus:border-orange-400"
          placeholder="https://github.com/username"
        />
      </div>

      <div>
        <label htmlFor="email" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
          Work Email
        </label>
        <input
          type="email"
          id="email"
          name="email"
          value={formData.email}
          onChange={handleChange}
          className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-orange-500 focus:outline-none dark:focus:border-orange-400"
          placeholder="your.email@example.com"
        />
      </div>

      <div>
        <label htmlFor="roles" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
          Professional Roles (comma-separated)
        </label>
        <input
          type="text"
          id="roles"
          value={Array.isArray(formData.roles) ? formData.roles.join(', ') : ''}
          onChange={handleRolesChange}
          className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-orange-500 focus:outline-none dark:focus:border-orange-400 resize-none"
          placeholder="Full Stack Developer, Java Developer, MERN Stack Developer, React Native Developer"
        />
      </div>

      {message && (
        <div
          className={`rounded-lg px-4 py-3 ${
            message.includes('successfully')
              ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300'
              : 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300'
          }`}
        >
          {message}
        </div>
      )}

      <div>
        <label className="flex items-center justify-between">
          <span className="block text-sm font-semibold text-slate-900 dark:text-white">Available for Work</span>
          <button
            type="button"
            onClick={() => handleAvailableChange(!formData.available)}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              formData.available
                ? 'bg-green-500'
                : 'bg-slate-300 dark:bg-slate-600'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                formData.available ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </label>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="w-full rounded-lg bg-gradient-to-r from-orange-500 to-orange-600 px-6 py-3 font-semibold text-white transition-all hover:shadow-lg hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {loading ? 'Updating...' : 'Update Profile'}
      </button>
    </form>
  );
}
