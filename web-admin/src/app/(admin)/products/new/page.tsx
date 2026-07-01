'use client';

import { ProductForm } from '@/components/ProductForm';

export default function NewProductPage() {
  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">Thêm sản phẩm</h1>
      <ProductForm />
    </div>
  );
}
