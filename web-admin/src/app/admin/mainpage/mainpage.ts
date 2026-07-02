import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminApiService } from '../admin-api.service';

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

  // Stats (loaded from admin APIs)
  totalUsers: number = 0;
  totalOrders: number = 0;
  totalProducts: number = 0;
  revenue: number = 0;

  activities: Array<any> = [];
  recentOrders: Array<any> = [];

  constructor(private router: Router, private adminApi: AdminApiService) {}

  ngOnInit() {
    this.profileName = 'Admin';
    this.today = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }).format(new Date());
    this.loadAdminStats();
    this.loadActivities();
    this.loadRecentOrders();
  }

  loadAdminStats() {
    this.adminApi.getUserCount().subscribe({ next: res => this.totalUsers = res?.count || 0, error: () => this.totalUsers = 0 });
    this.adminApi.getOrderCount().subscribe({ next: res => this.totalOrders = res?.count || 0, error: () => this.totalOrders = 0 });
    this.adminApi.getProductCount().subscribe({ next: res => this.totalProducts = res?.count || 0, error: () => this.totalProducts = 0 });
    this.adminApi.getRevenue().subscribe({ next: res => this.revenue = res?.revenue || 0, error: () => this.revenue = 0 });
  }

  loadActivities() {
    this.adminApi.getActivities().subscribe({ next: (a) => this.activities = a || [], error: () => this.activities = [] });
  }

  loadRecentOrders() {
    this.adminApi.getRecentOrders().subscribe({ next: (o) => this.recentOrders = o || [], error: () => this.recentOrders = [] });
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
}
