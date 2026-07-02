import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AuditService } from '../audit.service';

@Component({
  selector: 'app-shipping',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './shipping.html',
  styleUrl: './shipping.css',
})
export class Shipping implements OnInit {
  methods: any[] = [];
  loading = true;

  showForm = false;
  saving = false;
  errorMsg = '';
  editingId: string | null = null;

  form = { name: '', fee: 0, estimatedTime: '', area: '', description: '' };

  constructor(private adminApi: AdminApiService, private audit: AuditService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.adminApi.getShippingMethods().subscribe({
      next: (list) => {
        this.methods = (list || []).sort((a, b) => (Number(a.fee) || 0) - (Number(b.fee) || 0));
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.methods = [];
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openForm(): void {
    this.editingId = null;
    this.form = { name: '', fee: 0, estimatedTime: '', area: '', description: '' };
    this.errorMsg = '';
    this.showForm = true;
  }

  openEdit(m: any): void {
    this.editingId = m._id;
    this.form = {
      name: m.name || '',
      fee: Number(m.fee) || 0,
      estimatedTime: m.estimatedTime || '',
      area: m.area || '',
      description: m.description || '',
    };
    this.errorMsg = '';
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  save(): void {
    if (!this.form.name.trim()) {
      this.errorMsg = 'Vui lòng nhập tên đơn vị / phương thức vận chuyển.';
      return;
    }
    this.saving = true;
    this.errorMsg = '';

    const payload = {
      name: this.form.name.trim(),
      fee: Number(this.form.fee) || 0,
      estimatedTime: this.form.estimatedTime.trim(),
      area: this.form.area.trim(),
      description: this.form.description.trim(),
    };

    if (this.editingId) {
      const id = this.editingId;
      this.adminApi.updateShippingMethod(id, payload).subscribe({
        next: () => {
          const idx = this.methods.findIndex((x) => x._id === id);
          if (idx >= 0) this.methods[idx] = { ...this.methods[idx], ...payload };
          this.methods.sort((a, b) => (Number(a.fee) || 0) - (Number(b.fee) || 0));
          this.audit.log('shipping.update', payload.name, this.feeLabel(payload));
          this.saving = false;
          this.showForm = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể lưu (kiểm tra quyền Firestore).';
          this.cdr.markForCheck();
        },
      });
      return;
    }

    this.adminApi
      .addShippingMethod({ ...payload, createdAt: new Date().toISOString() })
      .subscribe({
        next: (id) => {
          this.methods.push({ _id: id, ...payload });
          this.methods.sort((a, b) => (Number(a.fee) || 0) - (Number(b.fee) || 0));
          this.audit.log('shipping.create', payload.name, this.feeLabel(payload));
          this.saving = false;
          this.showForm = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể lưu (kiểm tra quyền Firestore).';
          this.cdr.markForCheck();
        },
      });
  }

  remove(m: any): void {
    if (!m?._id) return;
    if (!confirm(`Xoá phương thức "${m.name}"?`)) return;
    this.adminApi.deleteShippingMethod(m._id).subscribe({
      next: () => {
        this.methods = this.methods.filter((x) => x._id !== m._id);
        this.audit.log('shipping.delete', m.name, '');
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMsg = 'Xoá thất bại.';
        this.cdr.markForCheck();
      },
    });
  }

  feeLabel(m: any): string {
    if (!m.fee || Number(m.fee) === 0) return 'Miễn phí';
    return this.formatCurrency(m.fee);
  }

  formatCurrency(n: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(n) || 0);
  }
}
