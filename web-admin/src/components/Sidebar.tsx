'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import type { Me } from '@/lib/types';

const NAV = [
  { href: '/dashboard', label: 'Tổng quan', icon: '📊' },
  { href: '/orders', label: 'Đơn hàng', icon: '🧾' },
  { href: '/users', label: 'Người dùng', icon: '👥' },
  { href: '/products', label: 'Sản phẩm', icon: '👕' },
];

export function Sidebar({ me }: { me: Me }) {
  const pathname = usePathname();
  const router = useRouter();
  const { signOut } = useAuth();

  const logout = async () => {
    await signOut();
    router.replace('/login');
  };

  return (
    <aside className="flex w-60 shrink-0 flex-col border-r bg-white">
      <div className="px-5 py-5">
        <span className="text-lg font-bold text-brand">TiredCity</span>
        <span className="ml-1 text-xs text-gray-400">Admin</span>
      </div>

      <nav className="flex-1 space-y-1 px-3">
        {NAV.map((item) => {
          const active = pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                active ? 'bg-brand text-white' : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <span>{item.icon}</span>
              {item.label}
            </Link>
          );
        })}
      </nav>

      <div className="border-t p-3">
        <div className="mb-2 px-2">
          <p className="truncate text-sm font-medium">{me.fullName ?? me.email}</p>
          <p className="text-xs text-gray-400">{me.role}</p>
        </div>
        <button
          onClick={logout}
          className="w-full rounded-lg px-3 py-2 text-left text-sm text-rose-600 hover:bg-rose-50"
        >
          Đăng xuất
        </button>
      </div>
    </aside>
  );
}
