'use client';

import React, { useState, useEffect } from 'react';
import { useTheme } from '@/context/ThemeContext';
import { FiCode, FiBriefcase, FiBook, FiSun, FiMoon, FiStar, FiAward } from 'react-icons/fi';

interface SidebarLayoutProps {
  children: React.ReactNode;
  hasProjects?: boolean;
  hasExperience?: boolean;
  hasEducation?: boolean;
  hasSkills?: boolean;
  hasCertifications?: boolean;
}

export default function SidebarLayout({ 
  children, 
  hasProjects = true,
  hasExperience = true,
  hasEducation = true,
  hasSkills = true,
  hasCertifications = true,
}: SidebarLayoutProps) {

  const { theme, toggleTheme } = useTheme();
  const [activeSection, setActiveSection] = useState<string | null>(null);
  const [isHydrated, setIsHydrated] = useState(false);

  const navItems = [
    { id: 'projects', icon: FiCode, label: 'Projects', show: hasProjects },
    { id: 'experience', icon: FiBriefcase, label: 'Experience', show: hasExperience },
    { id: 'education', icon: FiBook, label: 'Education', show: hasEducation },
    { id: 'skills', icon: FiStar, label: 'Skills', show: hasSkills },
    { id: 'certifications', icon: FiAward, label: 'Certifications', show: hasCertifications },
  ].filter(item => item.show);

  // ✅ Hydration fix - ensure consistency between server and client
  useEffect(() => {
    setIsHydrated(true);
  }, []);

  // ✅ Smooth scroll handler
  const handleScrollToSection = (id: string) => {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    }
  };

  // ✅ Intersection Observer (BEST WAY)
  useEffect(() => {
    const sections = navItems
      .map(item => document.getElementById(item.id))
      .filter(Boolean);

    if (sections.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        let visibleSection: string | null = null;

        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            visibleSection = entry.target.id;
          }
        });

        // Only set if it's in navItems
        if (visibleSection) {
          setActiveSection(visibleSection);
        } else {
          setActiveSection(null); // ✅ Fix: no section active
        }
      },
      {
        root: null,
        rootMargin: '-40% 0px -50% 0px', // controls when active triggers
        threshold: 0,
      }
    );

    sections.forEach((section) => observer.observe(section!));

    return () => {
      sections.forEach((section) => observer.unobserve(section!));
    };
  }, [navItems]);

  return (
    <>
      {/* 🔥 Floating Navbar - Only render after hydration */}
      {isHydrated && (
      <nav className="fixed bottom-4 left-1/2 -translate-x-1/2 z-40 flex flex-row gap-2 bg-gradient-to-b from-rose-500 to-rose-600 rounded-2xl p-1.5 shadow-lg">
        
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeSection === item.id;

          return (
            <button
              key={item.id}
              onClick={() => handleScrollToSection(item.id)}
              className={`
                group relative flex items-center justify-center h-8 w-8 rounded-lg 
                transition-all duration-200 ease-in-out
                ${isActive 
                  ? 'bg-white text-rose-500 scale-105' 
                  : 'bg-rose-500 text-white hover:bg-rose-600'
                }
              `}
            >
              <Icon className="h-4 w-4" />

              {/* Tooltip */}
              <div className="
                absolute top-14 px-3 py-2 rounded-lg whitespace-nowrap 
                text-sm font-medium pointer-events-none
                bg-slate-900 text-white opacity-0 group-hover:opacity-100
                transition-all duration-200
              ">
                {item.label}
              </div>
            </button>
          );
        })}

        {/* 🌙 Theme Toggle */}
        {/* <button
          onClick={toggleTheme}
          className="group relative flex items-center justify-center h-10 w-10 rounded-lg bg-rose-500 text-white hover:bg-rose-600 transition"
        >
          {theme === 'light' ? <FiMoon /> : <FiSun />}

          <div className="
            absolute top-14 px-3 py-2 rounded-lg whitespace-nowrap 
            text-sm font-medium pointer-events-none
            bg-slate-900 text-white opacity-0 group-hover:opacity-100
            transition-all duration-200
          ">
            {theme === 'light' ? 'Dark Mode' : 'Light Mode'}
          </div>
        </button> */}
      </nav>
      )}

      {/* ✅ IMPORTANT: Add scroll margin to ALL sections */}
      <main className="space-y-0 scroll-smooth">
  {React.Children.map(children, (child) => {
    if (React.isValidElement(child)) {
      const element = child as React.ReactElement<any>;

      return React.cloneElement(element, {
        className: `${element.props?.className || ''}`,
      });
    }
    return child;
  })}
</main>
    </>
  );
}