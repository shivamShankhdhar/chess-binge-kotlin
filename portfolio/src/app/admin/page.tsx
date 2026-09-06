'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { toast, Toaster } from 'sonner';
import ProfileForm from '@/components/admin/forms/ProfileForm';
import ProjectForm from '@/components/admin/forms/ProjectForm';
import EducationForm from '@/components/admin/forms/EducationForm';
import ExperienceForm from '@/components/admin/forms/ExperienceForm';
import SkillForm from '@/components/SkillForm';
import CertificationForm from '@/components/admin/forms/CertificationForm';
import ProjectCard from '@/components/cards/ProjectCard';
import EducationCard from '@/components/cards/EducationCard';
import ExperienceCard from '@/components/cards/ExperienceCard';
import SkillCard from '@/components/SkillCard';
import CertificationCard from '@/components/cards/CertificationCard';
import { FiLogOut } from 'react-icons/fi';

type Tab = 'profile' | 'projects' | 'education' | 'experience' | 'skills' | 'certifications';

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
}

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

interface Skill {
  _id: string;
  name: string;
  category: string;
  proficiency: string;
  icon?: string;
  image?: string;
  description?: string;
}

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

export default function AdminDashboard() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<Tab>('profile');
  const [projects, setProjects] = useState<Project[]>([]);
  const [education, setEducation] = useState<Education[]>([]);
  const [experience, setExperience] = useState<Experience[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [certifications, setCertifications] = useState<Certification[]>([]);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingData, setEditingData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [adminEmail, setAdminEmail] = useState<string>('');
  const [adminToken, setAdminToken] = useState<string>('');

  useEffect(() => {
    // Check if user is authenticated
    const token = localStorage.getItem('adminToken');
    const email = localStorage.getItem('adminEmail');
    if (!token) {
      router.push('/login');
      return;
    }
    setAdminEmail(email || '');
    setAdminToken(token);
    fetchAllData();
  }, []);

  const fetchAllData = async () => {
    try {
      setLoading(true);
      const [projectRes, eduRes, expRes, skillRes, certRes] = await Promise.all([
        fetch('/api/projects'),
        fetch('/api/education'),
        fetch('/api/experience'),
        fetch('/api/skills'),
        fetch('/api/certifications'),
      ]);

      if (projectRes.ok) setProjects(await projectRes.json());
      if (eduRes.ok) setEducation(await eduRes.json());
      if (expRes.ok) setExperience(await expRes.json());
      if (skillRes.ok) setSkills(await skillRes.json());
      if (certRes.ok) setCertifications(await certRes.json());
    } catch (error) {
      showMessage('Error fetching data', true);
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const showMessage = (msg: string, isError = false) => {
    if (isError) {
      toast.error(msg);
    } else {
      toast.success(msg);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminEmail');
    router.push('/login');
  };

  // ===== PROJECT HANDLERS =====
  const handleAddProject = async (data: any) => {
    try {
      const method = editingId ? 'PUT' : 'POST';
      const url = editingId ? `/api/projects/${editingId}` : '/api/projects';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (res.ok) {
        showMessage(editingId ? 'Project updated!' : 'Project added!');
        fetchAllData();
        setEditingId(null);
        setEditingData(null);
      } else {
        showMessage('Error saving project', true);
      }
    } catch (error) {
      showMessage('Error saving project', true);
      console.error(error);
    }
  };

  const handleDeleteProject = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      const res = await fetch(`/api/projects/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showMessage('Project deleted!');
        fetchAllData();
      } else {
        showMessage('Error deleting project', true);
      }
    } catch (error) {
      showMessage('Error deleting project', true);
    }
  };

  // ===== EDUCATION HANDLERS =====
  const handleAddEducation = async (data: any) => {
    try {
      const method = editingId ? 'PUT' : 'POST';
      const url = editingId ? `/api/education/${editingId}` : '/api/education';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (res.ok) {
        showMessage(editingId ? 'Education updated!' : 'Education added!');
        fetchAllData();
        setEditingId(null);
        setEditingData(null);
      } else {
        showMessage('Error saving education', true);
      }
    } catch (error) {
      showMessage('Error saving education', true);
      console.error(error);
    }
  };

  const handleDeleteEducation = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      const res = await fetch(`/api/education/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showMessage('Education deleted!');
        fetchAllData();
      } else {
        showMessage('Error deleting education', true);
      }
    } catch (error) {
      showMessage('Error deleting education', true);
    }
  };

  // ===== EXPERIENCE HANDLERS =====
  const handleAddExperience = async (data: any) => {
    try {
      const method = editingId ? 'PUT' : 'POST';
      const url = editingId ? `/api/experience/${editingId}` : '/api/experience';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (res.ok) {
        showMessage(editingId ? 'Experience updated!' : 'Experience added!');
        fetchAllData();
        setEditingId(null);
        setEditingData(null);
      } else {
        showMessage('Error saving experience', true);
      }
    } catch (error) {
      showMessage('Error saving experience', true);
      console.error(error);
    }
  };

  const handleDeleteExperience = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      const res = await fetch(`/api/experience/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showMessage('Experience deleted!');
        fetchAllData();
      } else {
        showMessage('Error deleting experience', true);
      }
    } catch (error) {
      showMessage('Error deleting experience', true);
    }
  };

  // ===== SKILL HANDLERS =====
  const handleAddSkill = async (data: any) => {
    try {
      console.log('Sending skill data:', data); // Debug log
      const method = editingId ? 'PUT' : 'POST';
      const url = editingId ? `/api/skills/${editingId}` : '/api/skills';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      const responseData = await res.json();

      if (res.ok) {
        showMessage(editingId ? 'Skill updated!' : 'Skill added!');
        fetchAllData();
        setEditingId(null);
        setEditingData(null);
      } else {
        // Display specific error message from API
        showMessage(responseData.error || 'Error saving skill', true);
      }
    } catch (error) {
      showMessage('Error saving skill', true);
      console.error(error);
    }
  };

  const handleDeleteSkill = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      const res = await fetch(`/api/skills/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showMessage('Skill deleted!');
        fetchAllData();
      } else {
        showMessage('Error deleting skill', true);
      }
    } catch (error) {
      showMessage('Error deleting skill', true);
    }
  };

  // ===== CERTIFICATION HANDLERS =====
  const handleAddCertification = async (data: any) => {
    try {
      const method = editingId ? 'PUT' : 'POST';
      const url = editingId ? `/api/certifications/${editingId}` : '/api/certifications';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (res.ok) {
        showMessage(editingId ? 'Certification updated!' : 'Certification added!');
        fetchAllData();
        setEditingId(null);
        setEditingData(null);
      } else {
        showMessage('Error saving certification', true);
      }
    } catch (error) {
      showMessage('Error saving certification', true);
      console.error(error);
    }
  };

  const handleDeleteCertification = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      const res = await fetch(`/api/certifications/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showMessage('Certification deleted!');
        fetchAllData();
      } else {
        showMessage('Error deleting certification', true);
      }
    } catch (error) {
      showMessage('Error deleting certification', true);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      {/* Header */}
      <header className="border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between">
            <Link href="/" className="flex items-center gap-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-orange-500 to-orange-600">
                <span className="text-lg font-bold text-white">SS</span>
              </div>
              <span className="text-xl font-bold text-slate-900">Admin Panel</span>
            </Link>
            <div className="flex items-center gap-4">
              <span className="text-sm text-slate-600">{adminEmail}</span>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 rounded-lg bg-red-100 dark:bg-red-900/30 px-4 py-2 text-sm font-semibold text-red-700 dark:text-red-300 transition-colors hover:bg-red-200 dark:hover:bg-red-900/50"
              >
                <FiLogOut className="h-4 w-4" />
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      <Toaster position="top-right" richColors />

      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        {/* Tabs */}
        <div className="mb-8 flex gap-4 border-b border-slate-200 overflow-x-auto">
          {(['profile', 'projects', 'education', 'experience', 'skills', 'certifications'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => {
                setActiveTab(tab);
                setEditingId(null);
                setEditingData(null);
              }}
              className={`border-b-2 px-4 py-3 font-semibold capitalize transition-colors whitespace-nowrap ${
                activeTab === tab
                  ? 'border-orange-500 text-orange-600'
                  : 'border-transparent text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="grid gap-12 lg:grid-cols-2">
          {/* Form Section */}
          <div>
            {activeTab === 'profile' && (
              <ProfileForm
                token={adminToken}
                onSuccess={() => showMessage('Profile updated successfully!')}
              />
            )}
            {activeTab === 'projects' && (
              <ProjectForm
                onSubmit={handleAddProject}
                initialData={editingData}
              />
            )}
            {activeTab === 'education' && (
              <EducationForm
                onSubmit={handleAddEducation}
                initialData={editingData}
              />
            )}
            {activeTab === 'experience' && (
              <ExperienceForm
                onSubmit={handleAddExperience}
                initialData={editingData}
              />
            )}
            {activeTab === 'skills' && (
              <SkillForm
                skill={editingData}
                onSave={handleAddSkill}
                onCancel={() => {
                  setEditingId(null);
                  setEditingData(null);
                }}
              />
            )}
            {activeTab === 'certifications' && (
              <CertificationForm
                certification={editingData}
                onSave={handleAddCertification}
                onCancel={() => {
                  setEditingId(null);
                  setEditingData(null);
                }}
              />
            )}
            {editingId && (activeTab === 'projects' || activeTab === 'education' || activeTab === 'experience') && (
              <button
                onClick={() => {
                  setEditingId(null);
                  setEditingData(null);
                }}
                className="mt-4 w-full rounded-lg border border-slate-300 px-6 py-3 font-semibold text-slate-700 transition-colors hover:bg-slate-50"
              >
                Cancel Editing
              </button>
            )}
          </div>

          {/* Content List Section */}
          <div className="space-y-6">
            {activeTab === 'projects' && (
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-slate-900">
                  Projects ({projects.length})
                </h3>
                {projects.length === 0 ? (
                  <p className="text-slate-600">No projects yet. Add one using the form!</p>
                ) : (
                  <div className="grid gap-4">
                    {projects.map((project) => (
                      <ProjectCard
                        key={project._id}
                        project={project}
                        onEdit={(proj: Project) => {
                          setEditingId(proj._id);
                          setEditingData(proj);
                        }}
                        onDelete={handleDeleteProject}
                        isAdmin
                      />
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'education' && (
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-slate-900">
                  Education ({education.length})
                </h3>
                {education.length === 0 ? (
                  <p className="text-slate-600">No education records yet. Add one using the form!</p>
                ) : (
                  <div className="grid gap-4">
                    {education.map((edu) => (
                      <EducationCard
                        key={edu._id}
                        education={edu}
                        onEdit={(e: Education) => {
                          setEditingId(e._id);
                          setEditingData(e);
                        }}
                        onDelete={handleDeleteEducation}
                        isAdmin
                      />
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'experience' && (
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-slate-900">
                  Experience ({experience.length})
                </h3>
                {experience.length === 0 ? (
                  <p className="text-slate-600">No experience records yet. Add one using the form!</p>
                ) : (
                  <div className="grid gap-4">
                    {experience.map((exp) => (
                      <ExperienceCard
                        key={exp._id}
                        experience={exp}
                        onEdit={(e: Experience) => {
                          setEditingId(e._id);
                          setEditingData(e);
                        }}
                        onDelete={handleDeleteExperience}
                        isAdmin
                      />
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'skills' && (
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-slate-900">
                  Skills ({skills.length})
                </h3>
                {skills.length === 0 ? (
                  <p className="text-slate-600">No skills yet. Add one using the form!</p>
                ) : (
                  <div className="grid gap-4 sm:grid-cols-2">
                    {skills.map((skill) => (
                      <SkillCard
                        key={skill._id}
                        skill={skill}
                        onEdit={(s: Skill) => {
                          setEditingId(s._id);
                          setEditingData(s);
                        }}
                        onDelete={handleDeleteSkill}
                        isAdmin
                      />
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'certifications' && (
              <div className="space-y-4">
                <h3 className="text-xl font-bold text-slate-900">
                  Certifications ({certifications.length})
                </h3>
                {certifications.length === 0 ? (
                  <p className="text-slate-600">No certifications yet. Add one using the form!</p>
                ) : (
                  <div className="grid gap-4">
                    {certifications.map((cert) => (
                      <CertificationCard
                        key={cert._id}
                        certification={cert}
                        onEdit={(c: Certification) => {
                          setEditingId(c._id);
                          setEditingData(c);
                        }}
                        onDelete={handleDeleteCertification}
                        isAdmin
                      />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
