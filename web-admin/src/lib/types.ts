// Kieu du lieu dung chung — anh xa sang document Firestore.
export type Role = 'CUSTOMER' | 'STAFF' | 'ADMIN' | 'AUDITOR';
export type Menh = 'KIM' | 'MOC' | 'THUY' | 'HOA' | 'THO';
export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'SOLD_OUT' | 'ARCHIVED';
export type OrderStatus =
  | 'PENDING' | 'CONFIRMED' | 'PACKING' | 'SHIPPING' | 'DELIVERED' | 'CANCELLED' | 'REFUNDED';

// Nguoi dang nhap hien tai (tu Firebase Auth + doc users/{uid})
export interface Me {
  id: string;
  email: string;
  fullName?: string;
  avatarUrl?: string;
  role: Role;
  menh?: Menh;
}

// users/{uid}
export interface AppUser {
  id: string;
  email: string;
  fullName?: string;
  avatarUrl?: string;
  role: Role;
  menh?: Menh;
  isActive: boolean;
  createdAt?: string; // ISO
}

// products/{id}
export interface Product {
  id: string;
  name: string;
  basePrice: number;
  salePrice?: number;
  status: ProductStatus;
  menh?: Menh;
  collectionName?: string;
  imageUrl?: string;
  totalStock?: number;
  createdAt?: string;
}

// orders/{id} — item nhung trong don (snapshot)
export interface OrderItem {
  productName: string;
  variantInfo?: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  orderCode: string;
  status: OrderStatus;
  total: number;
  userId?: string;
  userEmail?: string;
  userName?: string;
  itemCount?: number;
  items?: OrderItem[];
  note?: string;
  createdAt?: string; // ISO
}
