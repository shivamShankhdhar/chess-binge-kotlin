import ProjectCard from '@/components/cards/ProjectCard';

export default function ProjectsSection({ projects }: any) {
  if (!projects || !projects.length) return null;

  return (
    <section id="projects" className="border-t border-slate-200 dark:border-slate-800 px-6 py-20 sm:px-8 sm:py-24">
          <div className="mx-auto max-w-6xl">
            <div className="mb-12">
              <h2 className="text-4xl font-bold text-slate-900 dark:text-white">Featured Projects</h2>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-400">
                Here are some of my recent projects showcasing my skills and experience.
              </p>
            </div>
            <div className="grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {projects.map((project:any) => (
                <ProjectCard key={project._id} project={project} />
              ))}
            </div>
          </div>
        </section>
  );
}