'use client';

import Link from 'next/link';
import { useState } from 'react';
import { FiMenu, FiX } from 'react-icons/fi';

export default function Header() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 backdrop-blur-md bg-white/80 dark:bg-slate-900/80 border-b border-slate-200/50 dark:border-slate-700/50">
      <div className="mx-auto max-w-6xl px-6 sm:px-8">
        <div className="flex h-16 items-center justify-between">
          {/* Logo & Branding */}
          <Link href="/" className="group flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 shadow-lg group-hover:shadow-xl transition-all duration-300 group-hover:scale-105">
              <span className="text-lg font-bold text-white">SS</span>
            </div>
            <div className="hidden sm:block">
              <p className="text-lg font-bold bg-gradient-to-r from-slate-900 to-slate-700 dark:from-white dark:to-slate-200 bg-clip-text text-transparent">
                Shivam
              </p>
              <p className="text-xs font-semibold text-slate-600 dark:text-slate-400">Full Stack Developer</p>
            </div>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden gap-8 md:flex items-center">
            <Link 
              href="/#projects" 
              className="text-sm font-semibold text-slate-700 dark:text-slate-300 transition-all duration-300 hover:text-blue-600 dark:hover:text-blue-400 relative group"
            >
              Projects
              <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-500 to-purple-600 group-hover:w-full transition-all duration-300"></span>
            </Link>
            <Link 
              href="/#experience" 
              className="text-sm font-semibold text-slate-700 dark:text-slate-300 transition-all duration-300 hover:text-blue-600 dark:hover:text-blue-400 relative group"
            >
              Experience
              <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-500 to-purple-600 group-hover:w-full transition-all duration-300"></span>
            </Link>
            <Link 
              href="/#skills" 
              className="text-sm font-semibold text-slate-700 dark:text-slate-300 transition-all duration-300 hover:text-blue-600 dark:hover:text-blue-400 relative group"
            >
              Skills
              <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-500 to-purple-600 group-hover:w-full transition-all duration-300"></span>
            </Link>
            <Link 
              href="/#contact" 
              className="text-sm font-semibold text-slate-700 dark:text-slate-300 transition-all duration-300 hover:text-blue-600 dark:hover:text-blue-400 relative group"
            >
              Contact
              <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-500 to-purple-600 group-hover:w-full transition-all duration-300"></span>
            </Link>
          </nav>

          {/* Admin Button & Mobile Menu */}
          <div className="flex items-center gap-3">
            <Link
              href="/admin"
              className="hidden sm:inline-flex rounded-lg bg-gradient-to-r from-blue-600 to-blue-700 px-4 py-2 text-sm font-semibold text-white shadow-md transition-all duration-300 hover:shadow-lg hover:scale-105"
            >
              Admin
            </Link>

            <button
              className="md:hidden p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              onClick={() => setIsOpen(!isOpen)}
            >
              {isOpen ? (
                <FiX className="h-6 w-6 text-slate-900 dark:text-white" />
              ) : (
                <FiMenu className="h-6 w-6 text-slate-900 dark:text-white" />
              )}
            </button>
          </div>
        </div>

        {/* Mobile Navigation */}
        {isOpen && (
          <nav className="border-t border-slate-200/50 dark:border-slate-700/50 py-4 md:hidden space-y-2 pb-4">
            <Link 
              href="/#projects" 
              className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
            >
              Projects
            </Link>
            <Link 
              href="/#experience" 
              className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
            >
              Experience
            </Link>
            <Link 
              href="/#skills" 
              className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
            >
              Skills
            </Link>
            <Link 
              href="/#contact" 
              className="block px-3 py-2 rounded-lg text-sm font-semibold text-slate-700 dark:text-slate-300 hover:bg-blue-50 dark:hover:bg-blue-900/30 transition-colors"
            >
              Contact
            </Link>
            <Link
              href="/admin"
              className="block mt-2 w-full rounded-lg bg-gradient-to-r from-blue-600 to-blue-700 px-3 py-2 text-sm font-semibold text-white text-center transition-all duration-300"
            >
              Admin Panel
            </Link>
          </nav>
        )}
      </div>
    </header>
  );
}
