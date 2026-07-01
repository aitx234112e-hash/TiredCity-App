import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { RouterOutlet } from '@angular/router';
import { UserApiService } from '../../user-api.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    RouterOutlet
  ],
  templateUrl: './admin.html',
  styleUrl: './admin.css',
})
export class Admin {
  pageTitle: string = 'Trang chủ';
  profileName: string = 'Admin';
  sidebarCollapsed: boolean = false;

  searchQuery: string = '';
  showNotifications: boolean = false;
  unreadCount: number = 4;
  notifications: Array<{message: string; time: string; read: boolean}> = [
    { message: 'Đơn hàng mới #1042 vừa được tạo', time: '5 phút trước', read: false },
    { message: 'Người dùng mới đã đăng ký tài khoản', time: '12 phút trước', read: false },
    { message: 'Sản phẩm "Áo dài lụa" sắp hết hàng', time: '1 giờ trước', read: false },
    { message: 'Feedback mới từ khách hàng', time: '3 giờ trước', read: false },
  ];

  constructor(private router: Router, private userApi: UserApiService) {}

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  onSearch() {
    const q = this.searchQuery.trim();
    if (!q) return;
    // Navigate to relevant admin page based on search keywords
    const lower = q.toLowerCase();
    if (lower.includes('sản phẩm') || lower.includes('product')) {
      this.router.navigate(['/admin/products']);
    } else if (lower.includes('đơn hàng') || lower.includes('order')) {
      this.router.navigate(['/admin/orders']);
    } else if (lower.includes('tài khoản') || lower.includes('user')) {
      this.router.navigate(['/admin/users']);
    } else if (lower.includes('blog')) {
      this.router.navigate(['/admin/blogs']);
    } else if (lower.includes('feedback')) {
      this.router.navigate(['/admin/feedbacks']);
    }
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
  }

  markAllRead(event: Event) {
    event.stopPropagation();
    this.notifications.forEach(n => n.read = true);
    this.unreadCount = 0;
  }

  confirmLogout() {
    if (confirm('Bạn có chắc muốn đăng xuất?')) {
      this.logout();
    }
  }

  logout() {
    this.userApi.logout();
    this.router.navigate(['/login']);
  }
}
