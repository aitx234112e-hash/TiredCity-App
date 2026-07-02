import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditService } from '../audit.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-logs.html',
  styleUrl: './audit-logs.css',
})
export class AuditLogs implements OnInit {
  logs: any[] = [];
  filtered: any[] = [];
  loading = true;
  search = '';
  actionFilter = '';

  constructor(private audit: AuditService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.audit.getLogs().subscribe({
      next: (list) => {
        this.logs = list || [];
        this.applyFilter();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.logs = [];
        this.filtered = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get actions(): string[] {
    return Array.from(new Set(this.logs.map((l) => l.action).filter(Boolean))).sort();
  }

  applyFilter(): void {
    const kw = this.search.trim().toLowerCase();
    this.filtered = this.logs.filter((l) => {
      const matchAction = !this.actionFilter || l.action === this.actionFilter;
      const matchKw =
        !kw ||
        String(l.actor || '').toLowerCase().includes(kw) ||
        String(l.target || '').toLowerCase().includes(kw) ||
        String(l.detail || '').toLowerCase().includes(kw) ||
        String(l.action || '').toLowerCase().includes(kw);
      return matchAction && matchKw;
    });
  }

  actionLabel(action: string): string {
    const map: Record<string, string> = {
      login: 'Đăng nhập',
      logout: 'Đăng xuất',
      'order.status': 'Cập nhật đơn',
      'order.cancel': 'Huỷ đơn',
      'voucher.create': 'Tạo voucher',
      'voucher.delete': 'Xoá voucher',
      'shipping.create': 'Tạo vận chuyển',
      'shipping.delete': 'Xoá vận chuyển',
      'user.delete': 'Xoá user',
      'user.disable': 'Vô hiệu hoá user',
      'user.enable': 'Kích hoạt user',
      'product.create': 'Tạo sản phẩm',
      'product.delete': 'Xoá sản phẩm',
      'export.csv': 'Xuất CSV',
    };
    return map[action] || action;
  }

  actionClass(action: string): string {
    if (action.includes('delete') || action.includes('cancel') || action.includes('disable')) return 'danger';
    if (action.includes('create') || action.includes('enable')) return 'success';
    if (action.includes('login') || action.includes('logout')) return 'info';
    return 'neutral';
  }

  formatTime(t: string): string {
    if (!t) return '—';
    const d = new Date(t);
    if (isNaN(d.getTime())) return t;
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    }).format(d);
  }
}
