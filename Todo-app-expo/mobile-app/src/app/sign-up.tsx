import { useRouter } from 'expo-router';

import { AuthForm } from '@/components/auth-form';

export default function SignUpScreen() {
  const router = useRouter();
  return <AuthForm mode="sign-up" onSwitch={() => router.replace('/sign-in')} />;
}
