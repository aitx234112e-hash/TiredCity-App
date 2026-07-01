'use client';

import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listUsers, updateUserRole, setUserActive } from '@/lib/db';
import { formatDate } from '@/lib/format';
import type { AppUser, Role } from '@/lib/types';

const ROLES: Role[] = ['CUSTOMER', 'STAFF', 'ADMIN', 'AUDITOR'];
const ROLE_CLS: Record<Role, string> = {
  CUSTOMER: 'bg-gray-100 text-gray-600',
  STAFF: 'bg-blue-100 text-blue-700',
  ADMIN: 'bg-brand-soft text-brand',
  AUDITOR: 'bg-amber-100 text-amber-700',
};

export default function UsersPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');

  const { data: users = [], isLoading } = useQuery<AppUser[]>({
    queryKey: ['users'],
    queryFn: listUsers,
  });

  const filtered = useMemo(() => {
    const s = search.trim().toLowerCase();
    return users.filter((u) => {
      if (role && u.role !== role) return false;
      if (!s) return true;
      return u.email.toLowerCase().includes(s) || (u.fullName ?? '').toLowerCase().includes(s);
    });
  }, [users, search, role]);

  const changeRole = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) => updateUserRole(id, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
    onError: (e: any) => alert(e?.message ?? 'Cập nhật thất bại'),
  });

  const toggleActive = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) => setUserActive(id, isActive),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users'] }),
    onError: (e: any) => alert(e?.message ?? 'Cập nhật thất bại'),
  });

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">Người dùng</h1>

      <div className="flex flex-wrap gap-3">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tên hoặc email…"
          className="w-72 rounded-lg border px-3 py-2 text-sm"
        />
        <select value={role} onChange={(e) => setRole(e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
          <option value="">Tất cả vai trò</option>
          {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
      </div>

      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-500">
            <tr>
              <th className="px-4 py-3">Người dùng</th>
              <th className="px-4 py-3">Vai trò</th>
              <th className="px-4 py-3">Trạng thái</th>
              <th className="px-4 py-3">Ngày tạo</th>
              <th className="px-4 py-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Đang tải…</td></tr>}
            {filtered.map((u) => (
              <tr key={u.id} className={`border-t ${!u.isActive ? 'opacity-50' : ''}`}>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    {u.avatarUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={u.avatarUrl} alt="" className="h-9 w-9 rounded-full object-cover" />
                    ) : (
                      <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gray-100 text-gray-400">
                        {(u.fullName ?? u.email).charAt(0).toUpperCase()}
                      </div>
                    )}
                    <div>
                      <div className="font-medium">{u.fullName ?? '—'}</div>
                      <div className="text-xs text-gray-400">{u.email}</div>
                    </div>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <select
                    value={u.role}
                    onChange={(e) => changeRole.mutate({ id: u.id, role: e.target.value as Role })}
                    className={`rounded-full border-0 px-2 py-1 text-xs font-medium ${ROLE_CLS[u.role]}`}
                  >
                    {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                  </select>
                </td>
                <td className="px-4 py-3">
                  {u.isActive
                    ? <span className="rounded-full bg-green-100 px-2 py-1 text-xs font-medium text-green-700">Hoạt động</span>
                    : <span className="rounded-full bg-gray-200 px-2 py-1 text-xs font-medium text-gray-600">Đã khóa</span>}
                </td>
                <td className="px-4 py-3 text-gray-500">{u.createdAt ? formatDate(u.createdAt) : '—'}</td>
                <td className="px-4 py-3 text-right">
                  <button
                    onClick={() => toggleActive.mutate({ id: u.id, isActive: !u.isActive })}
                    className={u.isActive ? 'text-rose-600 hover:underline' : 'text-green-600 hover:underline'}
                  >
                    {u.isActive ? 'Khóa' : 'Mở khóa'}
                  </button>
                </td>
              </tr>
            ))}
            {!isLoading && filtered.length === 0 && <tr><td colSpan={5} className="px-4 py-8 text-center text-gray-400">Không có người dùng</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
