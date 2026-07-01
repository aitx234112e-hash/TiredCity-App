'use client';

import { useMemo, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listOrders, updateOrderStatus, bulkUpdateOrderStatus } from '@/lib/db';
import { formatVND, formatDate, ORDER_STATUS_LABEL } from '@/lib/format';
import type { Order, OrderStatus } from '@/lib/types';

const ALL_STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PACKING', 'SHIPPING', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

export default function OrdersPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [bulkStatus, setBulkStatus] = useState<OrderStatus>('CONFIRMED');

  const { data: orders = [], isLoading } = useQuery<Order[]>({
    queryKey: ['orders'],
    queryFn: listOrders,
  });

  const filtered = useMemo(() => {
    const s = search.trim().toLowerCase();
    return orders.filter((o) => {
      if (status && o.status !== status) return false;
      if (!s) return true;
      return (
        o.orderCode.toLowerCase().includes(s) ||
        (o.userEmail ?? '').toLowerCase().includes(s) ||
        (o.userName ?? '').toLowerCase().includes(s)
      );
    });
  }, [orders, search, status]);

  const updateOne = useMutation({
    mutationFn: ({ id, status }: { id: string; status: OrderStatus }) => updateOrderStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['orders'] }),
    onError: (e: any) => alert(e?.message ?? 'Cập nhật thất bại'),
  });

  const bulk = useMutation({
    mutationFn: () => bulkUpdateOrderStatus([...selected], bulkStatus),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['orders'] });
      alert(`Đã cập nhật ${selected.size} đơn.`);
      setSelected(new Set());
    },
    onError: (e: any) => alert(e?.message ?? 'Cập nhật thất bại'),
  });

  const toggle = (id: string) =>
    setSelected((s) => {
      const n = new Set(s);
      n.has(id) ? n.delete(id) : n.add(id);
      return n;
    });

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">Đơn hàng</h1>

      {/* Filter */}
      <div className="flex flex-wrap gap-3">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Mã đơn hoặc email khách…"
          className="w-72 rounded-lg border px-3 py-2 text-sm"
        />
        <select value={status} onChange={(e) => setStatus(e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
          <option value="">Tất cả trạng thái</option>
          {ALL_STATUSES.map((s) => <option key={s} value={s}>{ORDER_STATUS_LABEL[s].text}</option>)}
        </select>
      </div>

      {/* Bulk bar */}
      {selected.size > 0 && (
        <div className="flex items-center gap-3 rounded-lg bg-brand-soft px-4 py-2 text-sm">
          <span>Đã chọn {selected.size} đơn</span>
          <select value={bulkStatus} onChange={(e) => setBulkStatus(e.target.value as OrderStatus)} className="rounded border px-2 py-1">
            {ALL_STATUSES.map((s) => <option key={s} value={s}>{ORDER_STATUS_LABEL[s].text}</option>)}
          </select>
          <button onClick={() => bulk.mutate()} className="rounded bg-brand px-3 py-1 text-white">Áp dụng</button>
          <button onClick={() => setSelected(new Set())} className="text-gray-500">Bỏ chọn</button>
        </div>
      )}

      {/* Table */}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-500">
            <tr>
              <th className="px-4 py-3 w-8"></th>
              <th className="px-4 py-3">Mã đơn</th>
              <th className="px-4 py-3">Khách hàng</th>
              <th className="px-4 py-3">SP</th>
              <th className="px-4 py-3">Tổng</th>
              <th className="px-4 py-3">Ngày</th>
              <th className="px-4 py-3">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {isLoading && <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Đang tải…</td></tr>}
            {filtered.map((o) => (
              <tr key={o.id} className="border-t">
                <td className="px-4 py-3">
                  <input type="checkbox" checked={selected.has(o.id)} onChange={() => toggle(o.id)} />
                </td>
                <td className="px-4 py-3 font-medium">{o.orderCode}</td>
                <td className="px-4 py-3">
                  <div>{o.userName ?? '—'}</div>
                  <div className="text-xs text-gray-400">{o.userEmail}</div>
                </td>
                <td className="px-4 py-3">{o.itemCount ?? 0}</td>
                <td className="px-4 py-3">{formatVND(o.total)}</td>
                <td className="px-4 py-3 text-gray-500">{o.createdAt ? formatDate(o.createdAt) : '—'}</td>
                <td className="px-4 py-3">
                  <select
                    value={o.status}
                    onChange={(e) => updateOne.mutate({ id: o.id, status: e.target.value as OrderStatus })}
                    className={`rounded-full border-0 px-2 py-1 text-xs font-medium ${ORDER_STATUS_LABEL[o.status].cls}`}
                  >
                    {ALL_STATUSES.map((s) => <option key={s} value={s}>{ORDER_STATUS_LABEL[s].text}</option>)}
                  </select>
                </td>
              </tr>
            ))}
            {!isLoading && filtered.length === 0 && <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">Không có đơn hàng</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
