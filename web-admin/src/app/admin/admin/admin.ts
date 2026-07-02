import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
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
export class Admin implements OnInit {
  pageTitle: string = 'Trang chủ';
  profileName: string = 'Admin';
  sidebarCollapsed: boolean = false;
  darkMode: boolean = false;

  searchQuery: string = '';
  showNotifications: boolean = false;
  unreadCount: number = 4;
  notifications: Array<{message: string; time: string; read: boolean}> = [
    { message: 'Đơn hàng mới #1042 vừa được tạo', time: '5 phút trước', read: false },
    { message: 'Người dùng mới đã đăng ký tài khoản', time: '12 phút trước', read: false },
    { message: 'Sản phẩm "Áo dài lụa" sắp hết hàng', time: '1 giờ trước', read: false },
    { message: 'Feedback mới từ khách hàng', time: '3 giờ trước', read: false },
  ];

  // Tiêu đề header theo từng route
  private titleMap: Record<string, string> = {
    mainpage: 'Trang chủ',
    users: 'Tài khoản',
    products: 'Sản phẩm',
    orders: 'Đơn hàng',
    vouchers: 'Voucher',
    shipping: 'Vận chuyển',
    revenue: 'Doanh thu',
    events: 'Sự kiện',
    blogs: 'Blogs',
    feedbacks: 'Feedback',
    chatbot: 'Trợ lý Admin',
    reports: 'Báo cáo & Xuất dữ liệu',
    'audit-logs': 'Nhật ký hoạt động',
  };

  constructor(
    private router: Router,
    private userApi: UserApiService,
    private cdr: ChangeDetectorRef
  ) {}

  private updateTitle(url: string): void {
    const seg = (url.split('?')[0].split('/').filter(Boolean).pop() || 'mainpage');
    this.pageTitle = this.titleMap[seg] || 'Trang quản trị TiredCity';
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    // Cập nhật tiêu đề theo route hiện tại + mỗi lần điều hướng
    this.updateTitle(this.router.url);
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => this.updateTitle(e.urlAfterRedirects));

    if (typeof window === 'undefined') return;
    const saved = localStorage.getItem('admin-theme');
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.darkMode = saved ? saved === 'dark' : prefersDark;
    this.applyTheme();
  }

  toggleTheme() {
    this.darkMode = !this.darkMode;
    if (typeof window !== 'undefined') {
      localStorage.setItem('admin-theme', this.darkMode ? 'dark' : 'light');
    }
    this.applyTheme();
  }

  private applyTheme() {
    if (typeof document === 'undefined') return;
    document.documentElement.setAttribute('data-theme', this.darkMode ? 'dark' : 'light');
  }

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
