import CertificationCard from '@/components/cards/CertificationCard';

export default function CertificationsSection({ certifications }: any) {
  if (!certifications.length) return null;

  return (
    <section className="px-6 py-16 border-t">
      <div className="max-w-6xl mx-auto grid gap-6 md:grid-cols-3">
        {certifications.map((c: any) => (
          <CertificationCard key={c._id} certification={c} />
        ))}
      </div>
    </section>
  );
}