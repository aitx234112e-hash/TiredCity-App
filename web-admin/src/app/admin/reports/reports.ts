import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminApiService } from '../admin-api.service';
import { UserApiService } from '../../user-api.service';
import { ProductApiService } from '../../product-api.service';
import { AuditService } from '../audit.service';

interface Dataset {
  key: string;
  title: string;
  desc: string;
  count: number | null;
  loading: boolean;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.html',
  styleUrl: './reports.css',
})
export class Reports implements OnInit {
  datasets: Dataset[] = [
    { key: 'orders', title: 'Đơn hàng', desc: 'Mã đơn, khách hàng, tổng tiền, trạng thái, ngày đặt', count: null, loading: true },
    { key: 'products', title: 'Sản phẩm', desc: 'Tên, giá, tồn kho, danh mục', count: null, loading: true },
    { key: 'users', title: 'Người dùng', desc: 'Tên, email, SĐT, vai trò', count: null, loading: true },
  ];

  private cache: Record<string, any[]> = {};
  exporting = '';

  constructor(
    private adminApi: AdminApiService,
    private userApi: UserApiService,
    private productApi: ProductApiService,
    private audit: AuditService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.fetch('orders', this.adminApi.getOrders());
    this.fetch('products', this.productApi.getProducts());
    this.fetch('users', this.userApi.getUsers());
  }

  private fetch(key: string, obs: any): void {
    obs.subscribe({
      next: (list: any[]) => {
        this.cache[key] = list || [];
        this.setDataset(key, (list || []).length, false);
      },
      error: () => this.setDataset(key, 0, false),
    });
  }

  private setDataset(key: string, count: number, loading: boolean): void {
    const d = this.datasets.find((x) => x.key === key);
    if (d) {
      d.count = count;
      d.loading = loading;
    }
    this.cdr.markForCheck(); // zoneless: buộc render lại sau khi Firestore trả về
  }

  private toRows(key: string): { headers: string[]; rows: any[][] } {
    const data = this.cache[key] || [];
    if (key === 'orders') {
      return {
        headers: ['Mã đơn', 'Khách hàng', 'Tổng tiền', 'Trạng thái', 'Thanh toán', 'Ngày đặt'],
        rows: data.map((o) => [
          o.orderID || o._id || o.id || '',
          o.userName || o.customer || '',
          Number(o.totalPrice ?? o.total ?? 0),
          o.status || '',
          o.isPaid ? 'Đã thanh toán' : 'Chưa',
          o.createdAt ? new Date(o.createdAt).toLocaleDateString('vi-VN') : '',
        ]),
      };
    }
    if (key === 'products') {
      return {
        headers: ['Tên sản phẩm', 'Giá', 'Tồn kho', 'Danh mục'],
        rows: data.map((p) => [
          p.product_name || p.name || p.title || '',
          Number(p.unit_price ?? p.price ?? 0),
          Number(p.stocked_quantity ?? p.stock ?? 0),
          p.product_dept || p.category || '',
        ]),
      };
    }
    // users
    return {
      headers: ['Tên', 'Email', 'SĐT', 'Vai trò', 'Trạng thái'],
      rows: data.map((u) => [
        u.profileName || u.fullName || '',
        u.email || '',
        u.phone || '',
        u.role || 'user',
        u.disabled ? 'Vô hiệu hoá' : 'Hoạt động',
      ]),
    };
  }

  private csvEscape(val: any): string {
    const s = String(val ?? '');
    if (/[",\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"';
    return s;
  }

  export(key: string): void {
    if (typeof window === 'undefined') return;
    this.exporting = key;
    const { headers, rows } = this.toRows(key);
    const lines = [headers.join(','), ...rows.map((r) => r.map((c) => this.csvEscape(c)).join(','))];
    // BOM để Excel đọc đúng tiếng Việt UTF-8
    const csv = '﻿' + lines.join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const date = new Date().toISOString().slice(0, 10);
    a.download = `tiredcity_${key}_${date}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    this.audit.log('export.csv', key, `Xuất ${rows.length} dòng`);
    setTimeout(() => (this.exporting = ''), 800);
  }
}
