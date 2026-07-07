import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';
import { AuditService } from '../audit.service';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './events.html',
  styleUrl: './events.css',
})
export class Events implements OnInit {
  events: any[] = [];
  loading = true;

  showForm = false;
  saving = false;
  errorMsg = '';
  editingId: string | null = null;

  form = {
    title: '',
    date: '',
    location: '',
    description: '',
    image: '',
    isOnline: false
  };

  constructor(
    private adminApi: AdminApiService,
    private audit: AuditService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.adminApi.getEvents().subscribe({
      next: (list) => {
        this.events = (list || []).sort((a, b) =>
          String(b.date || '').localeCompare(String(a.date || ''))
        );
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.events = [];
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openForm(ev: any = null): void {
    if (ev) {
      this.editingId = ev._id;
      this.form = {
        title: ev.title || '',
        date: ev.date || '',
        location: ev.location || '',
        description: ev.description || '',
        image: ev.image || '',
        isOnline: !!ev.isOnline
      };
    } else {
      this.editingId = null;
      this.form = { title: '', date: '', location: '', description: '', image: '', isOnline: false };
    }
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
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Upload ảnh thất bại';
        this.cdr.detectChanges();
      },
    });
    input.value = '';
  }

  removeImage(): void {
    this.form.image = '';
  }

  save(): void {
    if (!this.form.title.trim()) {
      this.errorMsg = 'Vui lòng nhập tên sự kiện.';
      return;
    }
    this.saving = true;
    this.errorMsg = '';

    const payload = {
      ...this.form,
      title: this.form.title.trim(),
      location: this.form.location.trim(),
      description: this.form.description.trim(),
      updatedAt: new Date().toISOString()
    };

    if (this.editingId) {
      this.adminApi.updateEvent(this.editingId, payload).subscribe({
        next: () => {
          this.audit.log('event.update', this.form.title, 'Updated via Web Admin');
          this.load();
          this.saving = false;
          this.showForm = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể cập nhật sự kiện.';
          this.cdr.detectChanges();
        }
      });
    } else {
      const createPayload = {
        ...payload,
        createdAt: new Date().toISOString()
      };
      this.adminApi.addEvent(createPayload).subscribe({
        next: (id) => {
          this.audit.log('event.create', this.form.title, 'Created via Web Admin');
          this.events.unshift({ _id: id, ...createPayload });
          this.saving = false;
          this.showForm = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể lưu sự kiện.';
          this.cdr.detectChanges();
        },
      });
    }
  }

  remove(ev: any): void {
    if (!ev?._id) return;
    if (!confirm(`Xoá sự kiện "${ev.title}"?`)) return;
    this.adminApi.deleteEvent(ev._id).subscribe({
      next: () => {
        this.audit.log('event.delete', ev.title, 'Deleted via Web Admin');
        this.events = this.events.filter((e) => e._id !== ev._id);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMsg = 'Xoá thất bại.';
        this.cdr.detectChanges();
      },
    });
  }

  isUpcoming(ev: any): boolean {
    if (!ev?.date) return false;
    const d = new Date(ev.date);
    return !isNaN(d.getTime()) && d.getTime() >= Date.now();
  }

  formatDate(d: string): string {
    if (!d) return 'Chưa đặt ngày';
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(date);
  }
}
