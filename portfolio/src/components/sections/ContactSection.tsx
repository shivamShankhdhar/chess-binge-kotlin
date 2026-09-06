import { FiGithub, FiLinkedin } from 'react-icons/fi';
import { MdEmail } from 'react-icons/md';

export default function FooterSection({ profile }: any) {
  return (
    <footer className="py-10 border-t">
      <div className="flex justify-center gap-4">
        {profile?.linkedinUrl && <FiLinkedin />}
        {profile?.githubUrl && <FiGithub />}
        {profile?.email && <MdEmail />}
      </div>
    </footer>
  );
}