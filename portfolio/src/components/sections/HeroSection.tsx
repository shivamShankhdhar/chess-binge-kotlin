'use client';

import TypingAnimation from '@/components/TypingAnimation';
import { BsCopy } from 'react-icons/bs';

export default function HeroSection({ profile, adminEmail }: any) {
  return (
     <section className="relative min-h-screen overflow-hidden px-6 sm:px-8 flex items-start md:items-center justify-center py-20 md:pt-0">       {/* Background Gradient */}
            <div className="absolute inset-0 opacity-30 dark:opacity-10">
              <div className="absolute top-20 right-20 h-96 w-96 rounded-full bg-gradient-to-br from-rose-500 to-rose-600 blur-3xl"></div>
              <div className="absolute bottom-20 left-20 h-96 w-96 rounded-full bg-gradient-to-br from-rose-400 to-rose-500 blur-3xl"></div>
            </div>
    
            <div className="relative mx-auto max-w-6xl">
              <div className="grid items-center gap-16 md:grid-cols-[2fr_1fr]">

  {/* Avatar FIRST on mobile */}
  <div className="order-1 md:order-2 flex justify-center items-center">
    <div className="relative w-80 h-80 sm:w-96 sm:h-96 flex items-center justify-center">
      
      {/* Rings */}
      <div className="absolute inset-0 rounded-full border border-rose-300/40 dark:border-rose-500/30 animate-pulse"></div>
      <div className="absolute inset-0 rounded-full border border-rose-200/20 dark:border-rose-500/15 animate-ping"></div>

      {/* Avatar */}
      <div className="relative z-10 w-64 h-64 sm:w-72 sm:h-72 overflow-hidden rounded-full shadow-2xl">
        <img
          src="/avatar/shivam_avatar.png"
          alt="Shivam Shankhdhar"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-slate-900/20 to-transparent"></div>
      </div>
    </div>
  </div>

  {/* Text SECOND on mobile */}
  <div className="order-2 md:order-1 space-y-8 text-center md:text-left">
    
    <div className="space-y-4">
      <p className="text-sm font-semibold uppercase tracking-widest text-rose-600 dark:text-rose-400">
        Welcome to my World
      </p>

      <h1 className="text-4xl sm:text-5xl md:text-6xl font-bold">
        Hi, I'm
      </h1>

      <h1 className="bg-gradient-to-r text-4xl sm:text-5xl md:text-6xl font-bold from-rose-500 to-rose-600 bg-clip-text text-transparent">
        {profile?.name || 'Shivam Shankhdhar'}
      </h1>
    </div>

    {profile?.roles?.length > 0 && (
      <p className="text-lg sm:text-xl font-semibold">
        <TypingAnimation
          phrases={profile.roles}
          speed={60}
          delay={1500}
          className="bg-gradient-to-r from-rose-500 to-rose-600 bg-clip-text text-transparent"
        />
      </p>
    )}

    <p className="text-base sm:text-lg max-w-xl mx-auto md:mx-0 text-slate-600 dark:text-slate-400">
      {profile?.bio}
    </p>

    <div className="flex flex-wrap justify-center md:justify-start gap-3 pt-4">
      <button
        onClick={() => {
          navigator.clipboard.writeText(adminEmail);
          alert('Email copied to clipboard!');
        }}
        className="inline-flex items-center gap-2 rounded-xl text-sm bg-gradient-to-r from-slate-600 to-slate-700 px-4 py-2 text-white hover:scale-105 transition"
      >
        <BsCopy className="h-4 w-4" />
        Copy Email
      </button>

      <a
        href="#contact"
        className="inline-flex items-center gap-2 text-sm rounded-xl bg-gradient-to-r from-rose-500 to-rose-600 px-5 py-2 text-white hover:scale-105 transition"
      >
        Hire Me →
      </a>
    </div>
  </div>

</div>
            </div>
          </section>
  );
}