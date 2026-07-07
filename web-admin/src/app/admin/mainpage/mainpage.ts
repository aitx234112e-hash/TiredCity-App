import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminApiService } from '../admin-api.service';
import { Firestore } from '@angular/fire/firestore';
import { seedProducts } from '../seed-data';

@Component({
  selector: 'app-mainpage',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './mainpage.html',
  styleUrl: './mainpage.css',
})
export class Mainpage implements OnInit {
  profileName: string = '';
  today: string = '';

  totalUsers: number = 0;
  totalOrders: number = 0;
  totalProducts: number = 0;
  revenue: number = 0;

  activities: Array<any> = [];
  recentOrders: Array<any> = [];

  constructor(
    private router: Router,
    private adminApi: AdminApiService,
    private firestore: Firestore,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.profileName = 'Admin';
    this.today = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }).format(new Date());
    this.loadAdminStats();
    this.loadActivities();
    this.loadRecentOrders();
  }

  loadAdminStats() {
    this.adminApi.getUsers().subscribe((users: any[]) => {
      this.zone.run(() => { this.totalUsers = users.length; this.cdr.detectChanges(); });
    });
    this.adminApi.getProducts().subscribe((products: any[]) => {
      this.zone.run(() => { this.totalProducts = products.length; this.cdr.detectChanges(); });
    });
    this.adminApi.getOrders().subscribe((orders: any[]) => {
      this.zone.run(() => {
        this.totalOrders = orders.length;
        this.revenue = orders
          .filter(o => o.status === 'DELIVERED' || o.isPaid === true)
          .reduce((sum, o) => sum + (Number(o.totalPrice || 0)), 0);
        this.cdr.detectChanges();
      });
    });
  }

  loadActivities() {
    this.adminApi.getRecentOrders().subscribe((orders: any[]) => {
      this.zone.run(() => {
        this.activities = orders.slice(0, 6).map(o => ({
          timestamp: o.createdAt || new Date().toISOString(),
          description: `Đơn hàng ${o.orderCode || o._id.substring(0,8)} — Trạng thái: ${o.status}`
        }));
        this.cdr.detectChanges();
      });
    });
  }

  loadRecentOrders() {
    this.adminApi.getRecentOrders().subscribe((orders: any[]) => {
      this.zone.run(() => {
        this.recentOrders = orders;
        this.cdr.detectChanges();
      });
    });
  }

  formatCurrency(v: number) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0);
  }

  statusClass(status: string) {
    const s = (status || '').toLowerCase();
    if (s === 'pending') return 'status-pending';
    if (s === 'completed' || s === 'delivered') return 'status-completed';
    if (s === 'shipping' || s === 'shipped') return 'status-shipping';
    return 'status-default';
  }

  goTo(path: string) {
    this.router.navigate([path]);
  }

  async onQuickImport() {
    if (confirm('Bạn có chắc chắn muốn nạp toàn bộ sản phẩm từ dữ liệu Sheet lên Firebase không?')) {
      try {
        await seedProducts(this.firestore);
        this.loadAdminStats();
      } catch (error) {
        console.error('Lỗi khi nạp dữ liệu:', error);
        alert('Có lỗi xảy ra khi nạp dữ liệu. Vui lòng kiểm tra console.');
      }
    }
  }
}
