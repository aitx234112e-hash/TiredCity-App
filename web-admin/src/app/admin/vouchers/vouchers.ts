import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AuditService } from '../audit.service';

@Component({
  selector: 'app-vouchers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vouchers.html',
  styleUrl: './vouchers.css',
})
export class Vouchers implements OnInit {
  vouchers: any[] = [];
  loading = true;

  showForm = false;
  saving = false;
  errorMsg = '';

  form = { code: '', type: 'percent', value: 0, minOrder: 0, expiry: '', description: '', image: '' };

  constructor(private adminApi: AdminApiService, private audit: AuditService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.adminApi.getVouchers().subscribe({
      next: (list) => {
        this.vouchers = (list || []).sort((a, b) =>
          String(b.createdAt || '').localeCompare(String(a.createdAt || ''))
        );
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.vouchers = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openForm(): void {
    this.form = { code: '', type: 'percent', value: 0, minOrder: 0, expiry: '', description: '', image: '' };
    this.errorMsg = '';
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onImageChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.adminApi.uploadImage(file).subscribe({
      next: (res) => {
        this.form.image = res?.imageUrl || '';
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMsg = 'Upload ảnh thất bại';
      },
    });
    input.value = '';
  }

  removeImage(): void {
    this.form.image = '';
  }

  save(): void {
    if (!this.form.code.trim()) {
      this.errorMsg = 'Vui lòng nhập mã voucher.';
      return;
    }
    if (!this.form.value || this.form.value <= 0) {
      this.errorMsg = 'Giá trị giảm phải lớn hơn 0.';
      return;
    }
    this.saving = true;
    this.errorMsg = '';
    this.adminApi
      .addVoucher({
        code: this.form.code.trim().toUpperCase(),
        type: this.form.type,
        value: Number(this.form.value),
        minOrder: Number(this.form.minOrder) || 0,
        expiry: this.form.expiry,
        description: this.form.description.trim(),
        image: this.form.image || '',
        createdAt: new Date().toISOString(),
      })
      .subscribe({
        next: (id) => {
          const code = this.form.code.trim().toUpperCase();
          this.vouchers.unshift({ _id: id, ...this.form, code });
          this.audit.log('voucher.create', code, this.discountLabel(this.form));
          this.saving = false;
          this.showForm = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể lưu voucher (kiểm tra quyền Firestore).';
          this.cdr.markForCheck();
        },
      });
  }

  remove(v: any): void {
    if (!v?._id) return;
    if (!confirm(`Xoá voucher "${v.code}"?`)) return;
    this.adminApi.deleteVoucher(v._id).subscribe({
      next: () => {
        this.vouchers = this.vouchers.filter((x) => x._id !== v._id);
        this.audit.log('voucher.delete', v.code, '');
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMsg = 'Xoá thất bại.';
        this.cdr.markForCheck();
      },
    });
  }

  isExpired(v: any): boolean {
    if (!v?.expiry) return false;
    const d = new Date(v.expiry);
    return !isNaN(d.getTime()) && d.getTime() < Date.now();
  }

  discountLabel(v: any): string {
    if (v.type === 'percent') return `-${v.value}%`;
    return `-${this.formatCurrency(v.value)}`;
  }

  formatCurrency(n: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
  }

  formatDate(d: string): string {
    if (!d) return 'Không giới hạn';
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
  }
}
