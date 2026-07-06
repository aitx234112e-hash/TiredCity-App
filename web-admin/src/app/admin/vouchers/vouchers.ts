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
  isEditMode = false;
  editingId = '';
  saving = false;
  errorMsg = '';

  form = {
    _id: '',
    code: '',
    isActive: true,
    target: 'all',
    customTarget: '',
    type: 'percent',
    value: 0,
    minOrder: 0,
    startDate: '',
    startTime: '',
    expiry: '',
    endTime: '',
    usageLimit: 0,
    limitPerUser: 1,
    description: '',
    image: '',
    editCount: 0,
  };

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

  openForm(v?: any): void {
    if (v) {
      this.isEditMode = true;
      this.editingId = v._id;
      const isCustomTarget = !['all', 'new_user', 'loyal'].includes(v.target);
      this.form = {
        _id: v._id,
        code: v.code || '',
        isActive: v.isActive !== false,
        target: isCustomTarget ? 'custom' : (v.target || 'all'),
        customTarget: isCustomTarget ? v.target : '',
        type: v.type || 'percent',
        value: v.value || 0,
        minOrder: v.minOrder || 0,
        startDate: v.startDate || '',
        startTime: v.startTime || '',
        expiry: v.expiry || '',
        endTime: v.endTime || '',
        usageLimit: v.usageLimit || 0,
        limitPerUser: v.limitPerUser || 1,
        description: v.description || '',
        image: v.image || '',
        editCount: v.editCount || 0,
      };
      this.onValueInput(String(this.form.value));
      this.onMinOrderInput(String(this.form.minOrder));
    } else {
      this.isEditMode = false;
      this.editingId = '';
      this.form = {
        _id: '',
        code: '',
        isActive: true,
        target: 'all',
        customTarget: '',
        type: 'percent',
        value: 0,
        minOrder: 0,
        startDate: '',
        startTime: '',
        expiry: '',
        endTime: '',
        usageLimit: 0,
        limitPerUser: 1,
        description: '',
        image: '',
        editCount: 0,
      };
      this.formattedValue = '';
      this.formattedMinOrder = '';
    }
    this.errorMsg = '';
    this.showForm = true;
  }

  // Formatting for inputs
  formattedValue = '';
  formattedMinOrder = '';

  onValueInput(val: string): void {
    const clean = val.replace(/[^0-9]/g, '');
    if (this.form.type === 'percent') {
      let num = Number(clean) || 0;
      if (num > 100) num = 100;
      this.form.value = num;
      this.formattedValue = num > 0 ? num + ' %' : '';
    } else {
      this.form.value = Number(clean) || 0;
      this.formattedValue = this.form.value > 0 ? this.formatMoney(clean) + ' ₫' : '';
    }
  }

  onMinOrderInput(val: string): void {
    const clean = val.replace(/[^0-9]/g, '');
    this.form.minOrder = Number(clean) || 0;
    this.formattedMinOrder = this.form.minOrder > 0 ? this.formatMoney(clean) + ' ₫' : '';
  }

  formatMoney(val: string): string {
    if (!val) return '';
    return new Intl.NumberFormat('vi-VN').format(Number(val));
  }

  onTypeChange(): void {
    // Reset value formatting when type changes
    this.onValueInput(String(this.form.value));
  }

  validateRealtime(): void {
    this.errorMsg = '';
    if (this.form.startDate && this.form.expiry) {
      if (this.form.startDate > this.form.expiry) {
        this.errorMsg = 'Thời điểm kết thúc phải sau thời điểm bắt đầu.';
      } else if (this.form.startDate === this.form.expiry && this.form.startTime && this.form.endTime) {
        if (this.form.startTime >= this.form.endTime) {
          this.errorMsg = 'Giờ kết thúc phải sau giờ bắt đầu khi chọn cùng ngày.';
        }
      }
    }
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

    // Check date range
    if (this.form.startDate && this.form.expiry) {
      if (this.form.startDate > this.form.expiry) {
        this.errorMsg = 'Thời điểm kết thúc phải sau thời điểm bắt đầu.';
        return;
      }
      if (this.form.startDate === this.form.expiry && this.form.startTime && this.form.endTime) {
        if (this.form.startTime >= this.form.endTime) {
          this.errorMsg = 'Giờ kết thúc phải sau giờ bắt đầu khi chọn cùng ngày.';
          return;
        }
      }
    }

    this.saving = true;
    this.errorMsg = '';

    const payload: any = {
      code: this.form.code.trim().toUpperCase(),
      isActive: this.form.isActive,
      target: this.form.target === 'custom' ? this.form.customTarget : this.form.target,
      type: this.form.type,
      value: Number(this.form.value),
      minOrder: Number(this.form.minOrder) || 0,
      startDate: this.form.startDate,
      startTime: this.form.startTime,
      expiry: this.form.expiry,
      endTime: this.form.endTime,
      usageLimit: Number(this.form.usageLimit) || 0,
      limitPerUser: Number(this.form.limitPerUser) || 0,
      description: this.form.description.trim(),
      image: this.form.image || '',
    };

    if (this.isEditMode) {
      payload.editCount = (this.form.editCount || 0) + 1;
      this.adminApi.updateVoucher(this.editingId, payload).subscribe({
        next: () => {
          const idx = this.vouchers.findIndex((x) => x._id === this.editingId);
          if (idx !== -1) {
            this.vouchers[idx] = { ...this.vouchers[idx], ...payload };
          }
          this.audit.log('voucher.update', payload.code, this.discountLabel(payload));
          this.saving = false;
          this.showForm = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể cập nhật voucher.';
          this.cdr.markForCheck();
        },
      });
    } else {
      payload.usedCount = 0;
      payload.editCount = 0;
      payload.createdAt = new Date().toISOString();
      this.adminApi.addVoucher(payload).subscribe({
        next: (id) => {
          const code = payload.code;
          this.vouchers.unshift({ _id: id, ...payload });
          this.audit.log('voucher.create', code, this.discountLabel(payload));
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

  getStatus(v: any): string {
    if (!v.isActive) return 'Đã tắt';
    if (this.isExpired(v)) return 'Hết hạn';
    if (v.usageLimit > 0 && (v.usedCount || 0) >= v.usageLimit) return 'Hết lượt';
    return 'Hoạt động';
  }

  isExpired(v: any): boolean {
    if (!v?.expiry) return false;
    let expiryStr = v.expiry;
    if (v.endTime) {
      expiryStr += 'T' + v.endTime;
    } else {
      expiryStr += 'T23:59:59';
    }
    const d = new Date(expiryStr);
    return !isNaN(d.getTime()) && d.getTime() < Date.now();
  }

  discountLabel(v: any): string {
    if (v.type === 'percent') return `-${v.value}%`;
    return `-${this.formatCurrency(v.value)}`;
  }

  formatCurrency(n: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
  }

  formatDate(v: any): string {
    if (!v.expiry) return 'Không giới hạn';
    let out = this.formatDateOnly(v.expiry);
    if (v.endTime) out += ' ' + v.endTime;
    return out;
  }

  formatDateOnly(d: string): string {
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
  }

  getTargetLabel(t: string): string {
    if (t === 'all') return 'Tất cả';
    if (t === 'new_user') return 'Người mới';
    if (t === 'loyal') return 'Thân thiết';
    return t || 'Tất cả';
  }
}
