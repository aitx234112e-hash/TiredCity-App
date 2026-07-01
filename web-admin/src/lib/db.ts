// Lop truy cap du lieu Firestore cho web-admin.
// Du lieu nho (do an) nen fetch ca collection roi tinh toan phia client cho gon.
import {
  collection, doc, getDoc, getDocs, updateDoc, deleteDoc, setDoc,
  query, orderBy, Timestamp,
} from 'firebase/firestore';
import { db } from './firebase';
import type {
  AppUser, Order, OrderStatus, Product, ProductStatus, Role,
} from './types';

// ---- helpers ----
const toISO = (v: any): string | undefined => {
  if (!v) return undefined;
  if (v instanceof Timestamp) return v.toDate().toISOString();
  if (typeof v === 'string') return v;
  if (v?.seconds) return new Date(v.seconds * 1000).toISOString();
  return undefined;
};
const num = (v: any): number => (typeof v === 'number' ? v : Number(v ?? 0));

// ===================== ORDERS =====================
export async function listOrders(): Promise<Order[]> {
  const q = query(collection(db, 'orders'), orderBy('createdAt', 'desc'));
  const snap = await getDocs(q);
  return snap.docs.map((d) => {
    const x = d.data() as any;
    return {
      id: d.id,
      orderCode: x.orderCode ?? d.id.slice(0, 8).toUpperCase(),
      status: (x.status ?? 'PENDING') as OrderStatus,
      total: num(x.total),
      userId: x.userId,
      userEmail: x.userEmail,
      userName: x.userName,
      itemCount: x.itemCount ?? (Array.isArray(x.items) ? x.items.length : 0),
      items: x.items ?? [],
      note: x.note,
      createdAt: toISO(x.createdAt),
    };
  });
}

export async function updateOrderStatus(id: string, status: OrderStatus) {
  await updateDoc(doc(db, 'orders', id), { status });
}

export async function bulkUpdateOrderStatus(ids: string[], status: OrderStatus) {
  await Promise.all(ids.map((id) => updateOrderStatus(id, status)));
}

// ===================== USERS =====================
export async function listUsers(): Promise<AppUser[]> {
  const snap = await getDocs(collection(db, 'users'));
  return snap.docs
    .map((d) => {
      const x = d.data() as any;
      return {
        id: d.id,
        email: x.email ?? '',
        fullName: x.fullName,
        avatarUrl: x.avatarUrl,
        role: (x.role ?? 'CUSTOMER') as Role,
        menh: x.menh,
        isActive: x.isActive !== false,
        createdAt: toISO(x.createdAt),
      };
    })
    .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
}

export async function updateUserRole(id: string, role: Role) {
  await updateDoc(doc(db, 'users', id), { role });
}

export async function setUserActive(id: string, isActive: boolean) {
  await updateDoc(doc(db, 'users', id), { isActive });
}

// ===================== PRODUCTS =====================
export async function listProducts(): Promise<Product[]> {
  const snap = await getDocs(collection(db, 'products'));
  return snap.docs
    .map((d) => {
      const x = d.data() as any;
      return {
        id: d.id,
        name: x.name ?? '(chưa đặt tên)',
        basePrice: num(x.basePrice),
        salePrice: x.salePrice != null ? num(x.salePrice) : undefined,
        status: (x.status ?? 'DRAFT') as ProductStatus,
        menh: x.menh,
        collectionName: x.collectionName,
        imageUrl: x.imageUrl,
        totalStock: x.totalStock != null ? num(x.totalStock) : undefined,
        createdAt: toISO(x.createdAt),
      };
    })
    .sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));
}

export async function getProduct(id: string): Promise<Product | null> {
  const d = await getDoc(doc(db, 'products', id));
  if (!d.exists()) return null;
  const x = d.data() as any;
  return {
    id: d.id,
    name: x.name ?? '',
    basePrice: num(x.basePrice),
    salePrice: x.salePrice != null ? num(x.salePrice) : undefined,
    status: (x.status ?? 'DRAFT') as ProductStatus,
    menh: x.menh,
    collectionName: x.collectionName,
    imageUrl: x.imageUrl,
    totalStock: x.totalStock != null ? num(x.totalStock) : undefined,
    createdAt: toISO(x.createdAt),
  };
}

export type ProductInput = Omit<Product, 'id' | 'createdAt'>;

export async function createProduct(data: ProductInput) {
  const ref = doc(collection(db, 'products'));
  await setDoc(ref, { ...data, createdAt: Timestamp.now() });
  return ref.id;
}

export async function updateProduct(id: string, data: Partial<ProductInput>) {
  await updateDoc(doc(db, 'products', id), data as any);
}

export async function deleteProduct(id: string) {
  await deleteDoc(doc(db, 'products', id));
}

// ===================== DASHBOARD =====================
export interface Overview {
  revenue: number;
  totalOrders: number;
  pendingOrders: number;
  totalCustomers: number;
  activeProducts: number;
}

export async function getDashboard() {
  const [orders, users, products] = await Promise.all([listOrders(), listUsers(), listProducts()]);

  const overview: Overview = {
    revenue: orders.filter((o) => o.status === 'DELIVERED').reduce((s, o) => s + o.total, 0),
    totalOrders: orders.length,
    pendingOrders: orders.filter((o) => o.status === 'PENDING').length,
    totalCustomers: users.filter((u) => u.role === 'CUSTOMER').length,
    activeProducts: products.filter((p) => p.status === 'ACTIVE').length,
  };

  // Doanh thu 14 ngay gan nhat
  const days: { date: string; revenue: number; orders: number }[] = [];
  for (let i = 13; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    days.push({ date: key.slice(5), revenue: 0, orders: 0 });
  }
  const dayIndex: Record<string, number> = {};
  days.forEach((d, i) => (dayIndex[d.date] = i));
  for (const o of orders) {
    if (!o.createdAt) continue;
    const key = o.createdAt.slice(5, 10);
    const idx = dayIndex[key];
    if (idx == null) continue;
    days[idx].orders += 1;
    if (o.status === 'DELIVERED') days[idx].revenue += o.total;
  }

  // Don theo trang thai
  const statuses: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PACKING', 'SHIPPING', 'DELIVERED', 'CANCELLED', 'REFUNDED'];
  const byStatus = statuses.map((status) => ({
    status,
    count: orders.filter((o) => o.status === status).length,
  }));

  // Top san pham (tu items trong order)
  const soldMap: Record<string, { name: string; sold: number; revenue: number }> = {};
  for (const o of orders) {
    for (const it of o.items ?? []) {
      const key = it.productName;
      if (!soldMap[key]) soldMap[key] = { name: key, sold: 0, revenue: 0 };
      soldMap[key].sold += num(it.quantity);
      soldMap[key].revenue += num(it.lineTotal);
    }
  }
  const top = Object.values(soldMap).sort((a, b) => b.sold - a.sold).slice(0, 5);

  return { overview, revenue: days, byStatus, top };
}
