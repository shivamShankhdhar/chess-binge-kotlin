'use client';

import { useState, useEffect } from 'react';
import { useTheme } from '@/context/ThemeContext';
import SidebarLayout from '@/components/SidebarLayout';
import TypingAnimation from '@/components/TypingAnimation';
import ProjectCard from '@/components/cards/ProjectCard';
import ExperienceTimeline from '@/components/ExperienceTimeline';
import EducationCard from '@/components/cards/EducationCard';
import SkillCard from '@/components/SkillCard';
import CertificationCard from '@/components/cards/CertificationCard';
import { FiLinkedin, FiGithub } from 'react-icons/fi';
import { BsCopy } from 'react-icons/bs';
import { MdEmail } from 'react-icons/md';
import HeroSection from '@/components/sections/HeroSection';
import ProjectsSection from '@/components/sections/ProjectsSection';
import ExperienceSection from '@/components/sections/ExperienceSection';
import EducationSection from '@/components/sections/EducationSection';
import SkillsSection from '@/components/sections/SkillsSection';

export default function Home() {
  const { theme } = useTheme();
  const [isHydrated, setIsHydrated] = useState(false);
  const [projects, setProjects] = useState<any[]>([]);
  const [education, setEducation] = useState<any[]>([]);
  const [experience, setExperience] = useState<any[]>([]);
  const [skills, setSkills] = useState<any[]>([]);
  const [certifications, setCertifications] = useState<any[]>([]);
  const [profile, setProfile] = useState<any>({
    name: 'Shivam Shankhdhar',
    bio: 'Building beautiful, scalable, and user-friendly applications using modern technologies. Always passionate about learning and creating innovative solutions.',
    available: true,
    roles: ['Full Stack Developer', 'Java Developer', 'MERN Stack Developer', 'React Native Developer'],
    linkedinUrl: '',
    githubUrl: '',
  });
  const [adminEmail, setAdminEmail] = useState<string>('s.shankhdhar1981@gmail.com');
  const [loading, setLoading] = useState(true);  const [contactFormData, setContactFormData] = useState({ name: '', email: '', message: '' });
  const [contactLoading, setContactLoading] = useState(false);
  const [contactMessage, setContactMessage] = useState({ text: '', type: '' as 'success' | 'error' });
  
  // Hydration effect
  useEffect(() => {
    setIsHydrated(true);
  }, []);
  
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [projectRes, eduRes, expRes, skillRes, certRes, infoRes, profileRes] = await Promise.all([
          fetch('/api/projects'),
          fetch('/api/education'),
          fetch('/api/experience'),
          fetch('/api/skills'),
          fetch('/api/certifications'),
          fetch('/api/portfolio-info'),
          fetch('/api/profile'),
        ]);

        if (projectRes.ok) setProjects(await projectRes.json());
        if (eduRes.ok) setEducation(await eduRes.json());
        if (expRes.ok) setExperience(await expRes.json());
        if (skillRes.ok) setSkills(await skillRes.json());
        if (certRes.ok) setCertifications(await certRes.json());
        if (infoRes.ok) {
          const info = await infoRes.json();
          setAdminEmail(info.email);
        }
        if (profileRes.ok) {
          const profileData = await profileRes.json();
          setProfile(profileData.data);
        }
      } catch (error) {
        console.error('Error fetching data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  return (
    <SidebarLayout
      hasProjects={projects.length > 0}
      hasExperience={experience.length > 0}
      hasEducation={education.length > 0}
      hasSkills={skills.length > 0}
      hasCertifications={certifications.length > 0}
    >
      {/* Hero Section */}
     <HeroSection profile={profile} adminEmail={adminEmail} />

      {/* Projects Section */}
     <ProjectsSection projects={projects} />

      {/* Experience Section */}
     <ExperienceSection experience={experience} />

      {/* Education Section */}
      <EducationSection education={education} />

      {/* Skills Section */}
      <SkillsSection skills={skills} />

      {/* Certifications Section */}
      {certifications.length > 0 && (
        <section id="certifications" className="border-t border-slate-200 dark:border-slate-800 px-6 py-20 sm:px-8 sm:py-24">
          <div className="mx-auto max-w-6xl">
            <div className="mb-12">
              <h2 className="text-4xl font-bold text-slate-900 dark:text-white">Certifications</h2>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-400">
                Professional credentials and achievements.
              </p>
            </div>
            <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {certifications.map((cert) => (
                <CertificationCard key={cert._id} certification={cert} />
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Contact Section */}
      <section id="contact" className="border-t border-slate-200 dark:border-slate-800 px-6 py-20 sm:px-8 sm:py-24 bg-gradient-to-br from-slate-50 to-white dark:from-slate-900/50 dark:to-slate-950">
        <div className="mx-auto max-w-6xl">
          <div className="mb-12 text-center">
            <h2 className="text-4xl font-bold text-slate-900 dark:text-white">Let's Work Together</h2>
            <p className="mt-4 text-lg text-slate-600 dark:text-slate-400">
              Have a project in mind? Let's connect and create something amazing together.
            </p>
          </div>

          <div className="grid md:grid-cols-2 gap-12 lg:gap-20 items-center">
            {/* Form Side */}
            <div className="rounded-2xl border border-white/20 dark:border-white/10 bg-gradient-to-br from-white/80 to-white/60 dark:from-slate-800/50 dark:to-slate-900/50 backdrop-blur-xl p-8 shadow-lg">
              <form
                onSubmit={async (e) => {
                  e.preventDefault();
                  setContactLoading(true);
                  setContactMessage({ text: '', type: '' as 'success' | 'error' });

                  try {
                    const response = await fetch('/api/contact', {
                      method: 'POST',
                      headers: {
                        'Content-Type': 'application/json',
                      },
                      body: JSON.stringify(contactFormData),
                    });

                    const result = await response.json();

                    if (response.ok) {
                      setContactMessage({
                        text: result.message || 'Message sent successfully!',
                        type: 'success',
                      });
                      setContactFormData({ name: '', email: '', message: '' });
                    } else {
                      setContactMessage({
                        text: result.error || 'Failed to send message',
                        type: 'error',
                      });
                    }
                  } catch (error) {
                    setContactMessage({
                      text: 'Error sending message. Please try again.',
                      type: 'error',
                    });
                  } finally {
                    setContactLoading(false);
                    setTimeout(() => setContactMessage({ text: '', type: '' as 'success' | 'error' }), 5000);
                  }
                }}
                className="space-y-6"
              >
                <div>
                  <label htmlFor="name" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
                    Your Name
                  </label>
                  <input
                    type="text"
                    id="name"
                    value={contactFormData.name}
                    onChange={(e) => setContactFormData({ ...contactFormData, name: e.target.value })}
                    required
                    disabled={contactLoading}
                    className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-rose-500 focus:outline-none dark:focus:border-rose-400 disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="John Doe"
                  />
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
                    Your Email
                  </label>
                  <input
                    type="email"
                    id="email"
                    value={contactFormData.email}
                    onChange={(e) => setContactFormData({ ...contactFormData, email: e.target.value })}
                    required
                    disabled={contactLoading}
                    className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-rose-500 focus:outline-none dark:focus:border-rose-400 disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="john@example.com"
                  />
                </div>

                <div>
                  <label htmlFor="message" className="block text-sm font-semibold text-slate-900 dark:text-white mb-2">
                    Message
                  </label>
                  <textarea
                    id="message"
                    value={contactFormData.message}
                    onChange={(e) => setContactFormData({ ...contactFormData, message: e.target.value })}
                    required
                    disabled={contactLoading}
                    rows={5}
                    className="w-full rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 px-4 py-3 text-slate-900 dark:text-white placeholder-slate-400 dark:placeholder-slate-500 transition-all focus:border-rose-500 focus:outline-none dark:focus:border-rose-400 resize-none disabled:opacity-50 disabled:cursor-not-allowed"
                    placeholder="Tell me about your project..."
                  ></textarea>
                </div>

                {contactMessage.text && (
                  <div
                    className={`rounded-lg px-4 py-3 text-sm font-medium ${
                      contactMessage.type === 'success'
                        ? 'bg-green-50 dark:bg-green-900/30 text-green-800 dark:text-green-300 border border-green-200 dark:border-green-800'
                        : 'bg-red-50 dark:bg-red-900/30 text-red-800 dark:text-red-300 border border-red-200 dark:border-red-800'
                    }`}
                  >
                    {contactMessage.text}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={contactLoading}
                  className="w-full rounded-lg bg-gradient-to-r from-rose-500 to-rose-600 px-6 py-3 font-semibold text-white transition-all hover:shadow-lg hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed disabled:scale-100"
                >
                  {contactLoading ? 'Sending...' : 'Send Message'}
                </button>
              </form>
            </div>

            {/* Image Side */}
            <div className="relative hidden md:block">
              {/* Decorative background elements */}
              <div className="absolute -top-10 -right-10 w-40 h-40 bg-gradient-to-br from-rose-400/20 to-rose-600/20 rounded-full blur-3xl"></div>
              <div className="absolute -bottom-10 -left-10 w-40 h-40 bg-gradient-to-br from-blue-400/20 to-blue-600/20 rounded-full blur-3xl"></div>

              {/* Main SVG Illustration */}
              <div className="relative z-10 rounded-2xl border border-white/20 dark:border-white/10 bg-gradient-to-br from-white/40 to-white/20 dark:from-slate-800/40 dark:to-slate-900/40 backdrop-blur-xl p-6 shadow-lg h-full max-h-[580px] flex flex-col">
                <svg
                  className="w-full flex-1 max-h-[350px]"
                  viewBox="0 0 400 380"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  preserveAspectRatio="xMidYMid meet"
                >
                  {/* Desktop illustration */}
                  <rect x="50" y="120" width="300" height="180" rx="12" fill="url(#gradient)" opacity="0.8" />
                  <rect x="60" y="130" width="280" height="150" rx="8" fill="white" fillOpacity="0.1" />

                  {/* Screen content - animated bars */}
                  <rect x="80" y="155" width="240" height="8" rx="4" fill="currentColor" opacity="0.3" />
                  <rect x="80" y="175" width="200" height="6" rx="3" fill="currentColor" opacity="0.2" />
                  <rect x="80" y="188" width="220" height="6" rx="3" fill="currentColor" opacity="0.2" />
                  <rect x="80" y="201" width="180" height="6" rx="3" fill="currentColor" opacity="0.2" />
                  <rect x="80" y="214" width="200" height="6" rx="3" fill="currentColor" opacity="0.2" />
                  <rect x="80" y="227" width="190" height="6" rx="3" fill="currentColor" opacity="0.2" />
                  <rect x="80" y="240" width="240" height="8" rx="4" fill="currentColor" opacity="0.3" />

                  {/* Keyboard */}
                  <rect x="70" y="310" width="260" height="25" rx="4" fill="url(#keyboardGradient)" opacity="0.6" />
                  <circle cx="90" cy="322" r="3" fill="white" opacity="0.5" />
                  <circle cx="110" cy="322" r="3" fill="white" opacity="0.5" />
                  <circle cx="320" cy="322" r="3" fill="white" opacity="0.5" />

                  {/* Mouse */}
                  <ellipse cx="340" cy="245" rx="12" ry="18" fill="url(#mouseGradient)" opacity="0.7" />

                  {/* Message bubble */}
                  <path
                    d="M 120 50 L 280 50 Q 300 50 300 70 L 300 105 Q 300 120 280 120 L 140 120 L 120 135 L 120 120 Q 100 120 100 105 L 100 70 Q 100 50 120 50"
                    fill="url(#bubbleGradient)"
                    opacity="0.7"
                  />
                  <text x="200" y="80" textAnchor="middle" fill="white" fontSize="11" opacity="0.8">
                    Let's Connect
                  </text>

                  {/* Gradients */}
                  <defs>
                    <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#FF6B35" stopOpacity="0.2" />
                      <stop offset="100%" stopColor="#FFA96D" stopOpacity="0.1" />
                    </linearGradient>
                    <linearGradient id="keyboardGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" stopColor="#0EA5E9" stopOpacity="0.3" />
                      <stop offset="100%" stopColor="#3B82F6" stopOpacity="0.2" />
                    </linearGradient>
                    <linearGradient id="mouseGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#EC4899" stopOpacity="0.4" />
                      <stop offset="100%" stopColor="#F472B6" stopOpacity="0.2" />
                    </linearGradient>
                    <linearGradient id="bubbleGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#FF6B35" />
                      <stop offset="100%" stopColor="#FFA96D" />
                    </linearGradient>
                  </defs>
                </svg>

                {/* Info boxes below illustration */}
                <div className="mt-4 space-y-2">
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-gradient-to-r from-rose-500 to-rose-600"></div>
                    <p className="text-xs font-medium text-slate-700 dark:text-slate-300">Quick Response Time</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-gradient-to-r from-rose-500 to-rose-600"></div>
                    <p className="text-xs font-medium text-slate-700 dark:text-slate-300">Professional Solutions</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-gradient-to-r from-rose-500 to-rose-600"></div>
                    <p className="text-xs font-medium text-slate-700 dark:text-slate-300">24/7 Support Ready</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Empty State */}
      {!loading && projects.length === 0 && education.length === 0 && experience.length === 0 && skills.length === 0 && certifications.length === 0 && (
        <section className="px-6 py-20 sm:px-8 sm:py-24">
          <div className="mx-auto max-w-6xl text-center">
            <h2 className="text-3xl font-bold text-slate-900 dark:text-white">No content yet</h2>
            <p className="mt-4 text-lg text-slate-600 dark:text-slate-400">
              Visit the admin panel to add your projects, experience, education, skills, and certifications.
            </p>
          </div>
        </section>
      )}

      {/* Footer with Social Links and Email */}
      <footer className="border-t border-slate-200 dark:border-slate-800 bg-gradient-to-br from-slate-50 to-white dark:from-slate-900/50 dark:to-slate-950 px-6 py-5 sm:px-8 sm:py-16 pb-20">
        <div className="mx-auto max-w-6xl">
          <div className="flex flex-col items-center justify-center gap-8">
            {/* Social Links - Only render after hydration */}
            {isHydrated && (profile?.linkedinUrl || profile?.githubUrl || profile?.email) && (
            <div className="flex items-center flex flex-col gap-6">
                <p className="text-sm font-semibold text-slate-600 dark:text-slate-400 mb-2">Get in Touch</p>
<div className="flex flex-row justify-center items-center gap-x-4">
              {profile?.linkedinUrl && (
                <a
                  href={profile.linkedinUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group inline-flex items-center justify-center h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 text-white transition-all hover:shadow-lg hover:scale-110"
                  title="LinkedIn"
                >
                  <FiLinkedin className="h-6 w-6" />
                </a>
              )}
              {profile?.githubUrl && (
                <a
                  href={profile.githubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="group inline-flex items-center justify-center h-12 w-12 rounded-full bg-gradient-to-br from-slate-700 to-slate-800 dark:from-slate-600 dark:to-slate-700 text-white transition-all hover:shadow-lg hover:scale-110"
                  title="GitHub"
                >
                  <FiGithub className="h-6 w-6" />
                </a>
              )}
            

            {/* Email */}
            {profile?.email && (
              <div className="text-center">
                {/* <p className="text-sm font-semibold text-slate-600 dark:text-slate-400 mb-2">Get in Touch</p> */}
                <a
                  href={`mailto:${profile.email}`}
                  className="group inline-flex items-center justify-center h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-blue-600 text-white transition-all hover:shadow-lg hover:scale-110"

                  // className="inline-flex items-center gap-2 text-lg font-semibold text-slate-900 dark:text-white hover:text-rose-600 dark:hover:text-rose-400 transition-colors"
                >
                  <MdEmail/>
                </a>
              </div>
            )}
          </div>
          </div>
          
            )}
          </div>
        </div>
      </footer>
    </SidebarLayout>
  );
}