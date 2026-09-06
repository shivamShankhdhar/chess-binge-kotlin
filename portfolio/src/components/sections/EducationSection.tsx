import EducationCard from '@/components/cards/EducationCard';

export default function EducationSection({ education, isAdmin, onEdit, onDelete }: any) {
  if (!education || !education.length) return null;

  return (
    <section id="education" className="relative border-t border-slate-200 dark:border-slate-800 px-6 py-20 sm:px-8 sm:py-24 overflow-hidden">
      
      {/* Background Glow */}
      <div className="absolute inset-0 opacity-20 pointer-events-none">
        <div className="absolute -top-20 -left-20 w-96 h-96 rounded-full bg-gradient-to-br from-rose-500 to-red-600 blur-3xl"></div>
      </div>

      <div className="relative mx-auto max-w-6xl">
        <div className="mb-12">
          <span className="text-sm font-semibold uppercase tracking-widest text-rose-500">
            Learning & Growth
          </span>

          <h2 className="text-4xl font-bold text-white mt-2">
            Education
          </h2>

          <p className="mt-4 text-lg text-slate-400">
            My academic qualifications and achievements.
          </p>
        </div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-2">
          {education.map((edu: any) => (
            <EducationCard
              key={edu._id}
              education={edu}
              isAdmin={isAdmin}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      </div>
    </section>
  );
}