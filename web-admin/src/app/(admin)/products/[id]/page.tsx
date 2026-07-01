'use client';

import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { getProduct } from '@/lib/db';
import { ProductForm } from '@/components/ProductForm';

export default function EditProductPage() {
  const { id } = useParams<{ id: string }>();

  const { data, isLoading } = useQuery({
    queryKey: ['product', id],
    queryFn: () => getProduct(id),
  });

  if (isLoading) return <p className="text-gray-400">Đang tải…</p>;
  if (!data) return <p className="text-rose-600">Không tìm thấy sản phẩm.</p>;

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">Sửa: {data.name}</h1>
      <ProductForm product={data} />
    </div>
  );
}
