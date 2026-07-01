'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { createProduct, updateProduct, type ProductInput } from '@/lib/db';
import { PRODUCT_STATUS_LABEL } from '@/lib/format';
import type { Product, ProductStatus } from '@/lib/types';

const EMPTY: ProductInput = {
  name: '',
  basePrice: 0,
  salePrice: undefined,
  status: 'DRAFT',
  collectionName: '',
  imageUrl: '',
  totalStock: 0,
};

export function ProductForm({ product }: { product?: Product }) {
  const router = useRouter();
  const [form, setForm] = useState<ProductInput>(product ? { ...EMPTY, ...product } : EMPTY);
  const [saving, setSaving] = useState(false);

  const set = <K extends keyof ProductInput>(k: K, v: ProductInput[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) return alert('Nhập tên sản phẩm');
    setSaving(true);
    try {
      const payload: ProductInput = {
        ...form,
        basePrice: Number(form.basePrice) || 0,
        salePrice: form.salePrice ? Number(form.salePrice) : undefined,
        totalStock: Number(form.totalStock) || 0,
      };
      if (product) await updateProduct(product.id, payload);
      else await createProduct(payload);
      router.push('/products');
    } catch (e: any) {
      alert(e?.message ?? 'Lưu thất bại');
    } finally {
      setSaving(false);
    }
  };

  const field = 'w-full rounded-lg border px-3 py-2 text-sm';

  return (
    <form onSubmit={submit} className="max-w-2xl space-y-4">
      <div>
        <label className="mb-1 block text-sm font-medium">Tên sản phẩm</label>
        <input className={field} value={form.name} onChange={(e) => set('name', e.target.value)} />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="mb-1 block text-sm font-medium">Giá gốc (VND)</label>
          <input type="number" className={field} value={form.basePrice} onChange={(e) => set('basePrice', Number(e.target.value))} />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Giá khuyến mãi (tùy chọn)</label>
          <input type="number" className={field} value={form.salePrice ?? ''} onChange={(e) => set('salePrice', e.target.value ? Number(e.target.value) : undefined)} />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="mb-1 block text-sm font-medium">Bộ sưu tập</label>
          <input className={field} value={form.collectionName ?? ''} onChange={(e) => set('collectionName', e.target.value)} />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Tồn kho</label>
          <input type="number" className={field} value={form.totalStock ?? 0} onChange={(e) => set('totalStock', Number(e.target.value))} />
        </div>
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium">URL ảnh</label>
        <input className={field} value={form.imageUrl ?? ''} onChange={(e) => set('imageUrl', e.target.value)} placeholder="https://…" />
        {form.imageUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={form.imageUrl} alt="" className="mt-2 h-24 w-24 rounded object-cover" />
        )}
      </div>

      <div>
        <label className="mb-1 block text-sm font-medium">Trạng thái</label>
        <select className={field} value={form.status} onChange={(e) => set('status', e.target.value as ProductStatus)}>
          {Object.entries(PRODUCT_STATUS_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>

      <div className="flex gap-3 pt-2">
        <button type="submit" disabled={saving} className="rounded-lg bg-brand px-5 py-2 text-sm font-medium text-white hover:bg-brand-dark disabled:opacity-50">
          {saving ? 'Đang lưu…' : product ? 'Cập nhật' : 'Tạo sản phẩm'}
        </button>
        <button type="button" onClick={() => router.push('/products')} className="rounded-lg border px-5 py-2 text-sm">
          Hủy
        </button>
      </div>
    </form>
  );
}
