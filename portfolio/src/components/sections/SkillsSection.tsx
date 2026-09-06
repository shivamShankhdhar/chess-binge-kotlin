'use client';

import SkillCard from '@/components/SkillCard';
import { FiCode, FiServer, FiDatabase, FiTool, FiPackage, FiTerminal } from 'react-icons/fi';

const categoryConfig: Record<string, { icon: React.ReactNode; color: string; gradient: string; description: string }> = {
  Frontend: {
    icon: <FiCode className="h-6 w-6" />,
    color: 'from-blue-500 to-cyan-500',
    gradient: 'from-blue-50 to-cyan-50 dark:from-blue-900/20 dark:to-cyan-900/20',
    description: 'Client-side technologies & frameworks'
  },
  Backend: {
    icon: <FiServer className="h-6 w-6" />,
    color: 'from-green-500 to-emerald-500',
    gradient: 'from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20',
    description: 'Server-side & API development'
  },
  Database: {
    icon: <FiDatabase className="h-6 w-6" />,
    color: 'from-purple-500 to-pink-500',
    gradient: 'from-purple-50 to-pink-50 dark:from-purple-900/20 dark:to-pink-900/20',
    description: 'Data storage & management'
  },
  DevOps: {
    icon: <FiTool className="h-6 w-6" />,
    color: 'from-orange-500 to-red-500',
    gradient: 'from-orange-50 to-red-50 dark:from-orange-900/20 dark:to-red-900/20',
    description: 'Deployment & infrastructure'
  },
  Tools: {
    icon: <FiPackage className="h-6 w-6" />,
    color: 'from-indigo-500 to-violet-500',
    gradient: 'from-indigo-50 to-violet-50 dark:from-indigo-900/20 dark:to-violet-900/20',
    description: 'Development tools & utilities'
  },
  'Programming Language': {
    icon: <FiTerminal className="h-6 w-6" />,
    color: 'from-teal-500 to-cyan-500',
    gradient: 'from-teal-50 to-cyan-50 dark:from-teal-900/20 dark:to-cyan-900/20',
    description: 'Programming languages & paradigms'
  },
  Other: {
    icon: <FiCode className="h-6 w-6" />,
    color: 'from-slate-500 to-gray-500',
    gradient: 'from-slate-50 to-gray-50 dark:from-slate-900/20 dark:to-gray-900/20',
    description: 'Other technologies'
  }
};

export default function SkillsSection({ skills }: any) {
  if (!skills || !skills.length) return null;

  // Group skills by category
  const skillsByCategory = skills.reduce((acc: Record<string, any[]>, skill: any) => {
    const category = skill.category || 'Other';
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(skill);
    return acc;
  }, {});

  // Sort categories in a logical order
  const categoryOrder = ['Frontend', 'Backend', 'Programming Language', 'Database', 'DevOps', 'Tools', 'Other'];
  const sortedCategories = categoryOrder.filter(cat => skillsByCategory[cat]);

  return (
    <section id="skills" className="border-t border-slate-200 dark:border-slate-800 px-6 py-20 sm:px-8 sm:py-24">
      <div className="mx-auto max-w-6xl">
        <div className="mb-16">
          <span className="inline-block text-xs font-bold uppercase tracking-widest text-blue-600 dark:text-blue-400 mb-3">Technical Expertise</span>
          <h2 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 dark:from-white dark:to-slate-200 bg-clip-text text-transparent">Skills & Technologies</h2>
          <p className="mt-4 text-lg text-slate-600 dark:text-slate-400">
            A comprehensive overview of my technical expertise across different domains.
          </p>
        </div>

        <div className="space-y-16">
          {sortedCategories.map((category) => {
            const categorySkills = skillsByCategory[category];
            const config = categoryConfig[category];

            return (
              <div key={category} className="space-y-6">
                {/* Category Header - Simplified & Elegant */}
                <div className="relative group">
                  {/* Background gradient line */}
                  <div className={`absolute left-0 top-0 h-1 w-20 bg-gradient-to-r ${config.color} rounded-full group-hover:w-32 transition-all duration-500`}></div>
                  
                  <div className="pt-6 pb-4">
                    <div className="flex items-center gap-4 mb-3">
                      {/* Animated Icon */}
                      <div className={`p-3 rounded-lg bg-gradient-to-br ${config.color} text-white shadow-lg group-hover:shadow-xl group-hover:scale-110 group-hover:-rotate-6 transition-all duration-300`}>
                        {config.icon}
                      </div>
                      
                      {/* Category Name with Count */}
                      <div className="flex-1">
                        <h3 className={`text-2xl font-bold bg-gradient-to-r ${config.color} bg-clip-text text-transparent`}>{category}</h3>
                      </div>
                      
                      {/* Skill Count */}
                      <div className={`px-4 py-2 rounded-full bg-gradient-to-r ${config.color} text-white font-bold text-sm shadow-md`}>
                        {categorySkills.length} skills
                      </div>
                    </div>
                    
                    {/* Description */}
                    <p className="text-sm text-slate-600 dark:text-slate-400 ml-14">{config.description}</p>
                  </div>
                </div>

                {/* Skills Grid - Different layouts per category */}
                {category === 'Frontend' && (
                  <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}

                {category === 'Backend' && (
                  <div className="grid gap-6 md:grid-cols-2">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}

                {category === 'Programming Language' && (
                  <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}

                {category === 'Database' && (
                  <div className="grid gap-6 md:grid-cols-3">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}

                {category === 'DevOps' && (
                  <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}

                {category === 'Tools' && (
                  <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-4">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}

                {(category === 'Other' || !['Frontend', 'Backend', 'Programming Language', 'Database', 'DevOps', 'Tools'].includes(category)) && (
                  <div className="grid gap-6 md:grid-cols-3">
                    {categorySkills.map((skill: any) => (
                      <SkillCard key={skill._id} skill={skill} />
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}