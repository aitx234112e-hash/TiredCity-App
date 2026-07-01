'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

// Trang goc: dieu huong sang dashboard (guard se xu ly neu chua dang nhap)
export default function Home() {
  const router = useRouter();
  useEffect(() => {
    router.replace('/dashboard');
  }, [router]);
  return null;
}
