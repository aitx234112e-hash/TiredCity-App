import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminApiService } from '../admin-api.service';

interface MonthBar {
  label: string;
  value: number;
  pct: number;
}

@Component({
  selector: 'app-revenue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './revenue.html',
  styleUrl: './revenue.css',
})
export class Revenue implements OnInit {
  loading = true;

  totalRevenue = 0;
  paidRevenue = 0;
  orderCount = 0;
  paidCount = 0;
  avgOrderValue = 0;

  months: MonthBar[] = [];
  topOrders: any[] = [];

  constructor(private adminApi: AdminApiService) {}

  ngOnInit(): void {
    this.compute([]); // dựng khung 6 tháng ngay để biểu đồ luôn hiển thị
    this.adminApi.getOrders().subscribe({
      next: (orders) => {
        this.compute(orders || []);
        this.loading = false;
      },
      error: () => {
        this.compute([]);
        this.loading = false;
      },
    });
  }

  private orderTotal(o: any): number {
    return Number(o.totalPrice ?? o.total ?? o.amount ?? 0) || 0;
  }

  private isPaid(o: any): boolean {
    return !!(o.isPaid || o.paid || o.status === 'delivered' || o.status === 'completed');
  }

  private compute(orders: any[]): void {
    this.orderCount = orders.length;
    this.totalRevenue = orders.reduce((s, o) => s + this.orderTotal(o), 0);
    const paid = orders.filter((o) => this.isPaid(o));
    this.paidCount = paid.length;
    this.paidRevenue = paid.reduce((s, o) => s + this.orderTotal(o), 0);
    this.avgOrderValue = this.orderCount ? Math.round(this.totalRevenue / this.orderCount) : 0;

    // 6 tháng gần nhất
    const now = new Date();
    const buckets: { key: string; label: string; value: number }[] = [];
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      buckets.push({
        key: `${d.getFullYear()}-${d.getMonth()}`,
        label: `Th${d.getMonth() + 1}`,
        value: 0,
      });
    }
    orders.forEach((o) => {
      const raw = o.createdAt || o.created_at || o.date || o.orderDate;
      const d = raw ? new Date(raw?.seconds ? raw.seconds * 1000 : raw) : null;
      if (!d || isNaN(d.getTime())) return;
      const key = `${d.getFullYear()}-${d.getMonth()}`;
      const b = buckets.find((x) => x.key === key);
      if (b) b.value += this.orderTotal(o);
    });
    const max = Math.max(1, ...buckets.map((b) => b.value));
    this.months = buckets.map((b) => ({
      label: b.label,
      value: b.value,
      pct: Math.round((b.value / max) * 100),
    }));

    this.topOrders = [...orders]
      .sort((a, b) => this.orderTotal(b) - this.orderTotal(a))
      .slice(0, 6);
  }

  formatCurrency(v: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
  }

  formatCompact(v: number): string {
    if (v >= 1_000_000) return (v / 1_000_000).toFixed(1).replace('.0', '') + 'tr';
    if (v >= 1_000) return Math.round(v / 1_000) + 'k';
    return String(v || 0);
  }
}
