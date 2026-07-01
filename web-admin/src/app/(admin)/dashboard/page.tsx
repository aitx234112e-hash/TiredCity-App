'use client';

import { useQuery } from '@tanstack/react-query';
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, CartesianGrid,
  BarChart, Bar,
} from 'recharts';
import { getDashboard } from '@/lib/db';
import { formatVND } from '@/lib/format';
import { ORDER_STATUS_LABEL } from '@/lib/format';

function StatCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-xl border bg-white p-5">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value}</p>
      {hint && <p className="mt-1 text-xs text-gray-400">{hint}</p>}
    </div>
  );
}

export default function DashboardPage() {
  const { data } = useQuery({ queryKey: ['dashboard'], queryFn: getDashboard });
  const o = data?.overview;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Tổng quan</h1>

      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Doanh thu (đã giao)" value={o ? formatVND(o.revenue) : '—'} />
        <StatCard label="Tổng đơn hàng" value={o ? String(o.totalOrders) : '—'} hint={o ? `${o.pendingOrders} chờ xác nhận` : ''} />
        <StatCard label="Khách hàng" value={o ? String(o.totalCustomers) : '—'} />
        <StatCard label="SP đang bán" value={o ? String(o.activeProducts) : '—'} />
      </div>

      {/* Revenue line chart */}
      <div className="rounded-xl border bg-white p-5">
        <h2 className="mb-4 font-semibold">Doanh thu 14 ngày gần nhất</h2>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data?.revenue ?? []}>
              <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => `${v / 1000}k`} />
              <Tooltip formatter={(v: number) => formatVND(v)} />
              <Line type="monotone" dataKey="revenue" stroke="#C0392B" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Orders by status */}
        <div className="rounded-xl border bg-white p-5">
          <h2 className="mb-4 font-semibold">Đơn theo trạng thái</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={(data?.byStatus ?? []).map((s) => ({ ...s, label: ORDER_STATUS_LABEL[s.status]?.text ?? s.status }))}>
                <CartesianGrid strokeDasharray="3 3" stroke="#eee" />
                <XAxis dataKey="label" tick={{ fontSize: 9 }} interval={0} angle={-15} textAnchor="end" height={50} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#C0392B" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Top products */}
        <div className="rounded-xl border bg-white p-5">
          <h2 className="mb-4 font-semibold">Top sản phẩm bán chạy</h2>
          <ul className="space-y-3">
            {(data?.top ?? []).map((p, i) => (
              <li key={i} className="flex items-center justify-between text-sm">
                <span className="truncate">{i + 1}. {p.name}</span>
                <span className="shrink-0 text-gray-500">{p.sold} đã bán · {formatVND(p.revenue)}</span>
              </li>
            ))}
            {data?.top.length === 0 && <li className="text-sm text-gray-400">Chưa có dữ liệu</li>}
          </ul>
        </div>
      </div>
    </div>
  );
}
