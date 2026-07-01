'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { listProducts, deleteProduct } from '@/lib/db';
import { formatVND, PRODUCT_STATUS_LABEL } from '@/lib/format';
import type { Product } from '@/lib/types';

export default function ProductsPage() {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');

  const { data: products = [], isLoading } = useQuery<Product[]>({
    queryKey: ['products'],
    queryFn: listProducts,
  });

  const filtered = useMemo(() => {
    const s = search.trim().toLowerCase();
    return products.filter((p) => {
      if (status && p.status !== status) return false;
      if (!s) return true;
      return p.name.toLowerCase().includes(s);
    });
  }, [products, search, status]);

  const del = useMutation({
    mutationFn: (id: string) => deleteProduct(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
    onError: (e: any) => alert(e?.message ?? 'Xóa thất bại'),
  });

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Sản phẩm</h1>
        <Link href="/products/new" className="rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white hover:bg-brand-dark">
          + Thêm sản phẩm
        </Link>
      </div>

      {/* Filter */}
      <div className="flex flex-wrap gap-3">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm theo tên…"
          className="w-64 rounded-lg border px-3 py-2 text-sm"
        />
        <select value={status} onChange={(e) => setStatus(e.target.value)} className="rounded-lg border px-3 py-2 text-sm">
          <option value="">Tất cả trạng thái</option>
          {Object.entries(PRODUCT_STATUS_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>

      {/* Table */}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-500">
            <tr>
              <th className="px-4 py-3">Sản phẩm</th>
              <th className="px-4 py-3">BST</th>
              <th className="px-4 py-3">Giá</th>
              <th className="px-4 py-3">Tồn</th>
              <th className="px-4 py-3">Trạng thái</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {isLoading && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Đang tải…</td></tr>}
            {filtered.map((p) => (
              <tr key={p.id} className="border-t">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    {p.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={p.imageUrl} alt="" className="h-10 w-10 rounded object-cover" />
                    ) : (
                      <div className="h-10 w-10 rounded bg-gray-100" />
                    )}
                    <span className="font-medium">{p.name}</span>
                  </div>
                </td>
                <td className="px-4 py-3 text-gray-500">{p.collectionName ?? '—'}</td>
                <td className="px-4 py-3">{formatVND(p.salePrice ?? p.basePrice)}</td>
                <td className="px-4 py-3">{p.totalStock ?? 0}</td>
                <td className="px-4 py-3">{PRODUCT_STATUS_LABEL[p.status]}</td>
                <td className="px-4 py-3 text-right">
                  <Link href={`/products/${p.id}`} className="text-brand hover:underline">Sửa</Link>
                  <button
                    onClick={() => confirm('Xóa sản phẩm này?') && del.mutate(p.id)}
                    className="ml-3 text-rose-600 hover:underline"
                  >
                    Xóa
                  </button>
                </td>
              </tr>
            ))}
            {!isLoading && filtered.length === 0 && <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Không có sản phẩm</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}
