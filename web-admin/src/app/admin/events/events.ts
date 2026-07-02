import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminApiService } from '../admin-api.service';

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

  form = { title: '', date: '', location: '', description: '', image: '' };

  constructor(private adminApi: AdminApiService, private cdr: ChangeDetectorRef) {}

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

  openForm(): void {
    this.form = { title: '', date: '', location: '', description: '', image: '' };
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
    this.adminApi
      .addEvent({
        title: this.form.title.trim(),
        date: this.form.date,
        location: this.form.location.trim(),
        description: this.form.description.trim(),
        image: this.form.image || '',
        createdAt: new Date().toISOString(),
      })
      .subscribe({
        next: (id) => {
          this.events.unshift({ _id: id, ...this.form });
          this.saving = false;
          this.showForm = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.saving = false;
          this.errorMsg = 'Không thể lưu sự kiện (kiểm tra quyền Firestore).';
          this.cdr.detectChanges();
        },
      });
  }

  remove(ev: any): void {
    if (!ev?._id) return;
    if (!confirm(`Xoá sự kiện "${ev.title}"?`)) return;
    this.adminApi.deleteEvent(ev._id).subscribe({
      next: () => {
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
