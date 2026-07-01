'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import { Sidebar } from '@/components/Sidebar';

// Layout dung chung cho moi trang admin + guard client-side
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { me, loading } = useAuth();

  useEffect(() => {
    if (!loading && !me) router.replace('/login');
  }, [loading, me, router]);

  if (loading) {
    return <div className="flex min-h-screen items-center justify-center text-gray-400">Đang tải…</div>;
  }
  if (!me) return null;

  return (
    <div className="flex min-h-screen">
      <Sidebar me={me} />
      <main className="flex-1 overflow-x-hidden px-8 py-6">{children}</main>
    </div>
  );
}
