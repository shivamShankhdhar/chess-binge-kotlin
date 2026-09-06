import ExperienceTimeline from '@/components/ExperienceTimeline';

export default function ExperienceSection({ experience }: any) {
  if (!experience ||!experience.length) return null;

  return (
    <section id="experience" className="relative border-t border-slate-200 dark:border-slate-800 bg-gradient-to-br from-slate-50 to-rose-50/30 dark:from-slate-900/50 dark:to-rose-900/20 px-6 py-12 sm:px-8 sm:py-16 overflow-hidden">
          {/* Decorative gradient blob */}
          <div className="absolute inset-0 opacity-20 dark:opacity-10 pointer-events-none">
            <div className="absolute -bottom-20 -right-20 w-96 h-96 rounded-full bg-gradient-to-tl from-rose-400 to-rose-500 blur-3xl"></div>
          </div>
          <div className="relative mx-auto max-w-6xl">
            <div className="mb-12">
              <span className="inline-block text-sm font-semibold uppercase tracking-widest text-rose-600 dark:text-rose-400 mb-2">Professional Journey</span>
              <h2 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 dark:from-white dark:to-slate-200 bg-clip-text text-transparent">Work Experience</h2>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-400">
                My professional journey and roles I've held.
              </p>
            </div>
            <ExperienceTimeline experiences={experience} />
          </div>
        </section>
  );
}