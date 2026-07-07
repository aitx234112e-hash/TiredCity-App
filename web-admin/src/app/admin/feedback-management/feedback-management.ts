import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { FeedbackApiService } from '../../feedback-api.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-feedback-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './feedback-management.html',
  styleUrls: ['./feedback-management.css']
})
export class FeedbackManagement implements OnInit {

  feedbacks: any[] = [];
  filtered: any[] = [];
  paginatedFeedbacks: any[] = [];

  searchText = '';
  filterReplied = 'all';

  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  loading = false;
  successMsg = '';
  errorMsg = '';

  // Detail modal
  showDetail = false;
  selectedFeedback: any = null;
  adminReply = '';

  // Delete confirm modal
  showDeleteConfirm = false;
  deletingFeedback: any = null;

  constructor(
    private api: FeedbackApiService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadFeedback();
  }

  loadFeedback() {
    this.loading = true;
    this.errorMsg = '';

    this.api.getFeedback().subscribe({
      next: (res) => {
        this.zone.run(() => {
          // Sắp xếp local theo thời gian mới nhất
          this.feedbacks = (res as any[]).sort((a, b) => {
            const dateA = new Date(a.createdAt || 0).getTime();
            const dateB = new Date(b.createdAt || 0).getTime();
            return dateB - dateA;
          });
          this.applyFilter();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.zone.run(() => {
          this.loading = false;
          console.error('Feedback Load Error:', err);
          this.errorMsg = 'Lỗi tải dữ liệu phản hồi từ Firestore.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  applyFilter() {
    const keyword = this.searchText.trim().toLowerCase();
    this.filtered = this.feedbacks.filter(f => {
      let matchStatus = true;
      if (this.filterReplied === 'replied') matchStatus = f.replied === true;
      if (this.filterReplied === 'notReplied') matchStatus = !f.replied;

      const matchKeyword = !keyword ||
        (f.fullName || '').toLowerCase().includes(keyword) ||
        (f.name || '').toLowerCase().includes(keyword) ||
        (f.userName || '').toLowerCase().includes(keyword) ||
        (f.email || '').toLowerCase().includes(keyword) ||
        (f.phone || '').toLowerCase().includes(keyword) ||
        (f.message || '').toLowerCase().includes(keyword);

      return matchStatus && matchKeyword;
    });

    this.currentPage = 1;
    this.totalPages = Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
    this.updatePagination();
  }

  updatePagination() {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginatedFeedbacks = this.filtered.slice(start, start + this.pageSize);
  }

  previousPage() {
    if (this.currentPage <= 1) return;
    this.currentPage--;
    this.updatePagination();
  }

  nextPage() {
    if (this.currentPage >= this.totalPages) return;
    this.currentPage++;
    this.updatePagination();
  }

  countReplied(): number {
    return this.feedbacks.filter(f => f.replied).length;
  }

  // ── Mark replied (saves to backend) ──
  markReplied(f: any) {
    if (!f) return;
    const reply = this.adminReply.trim();
    if (!reply) {
      this.errorMsg = 'Vui lòng nhập nội dung phản hồi.';
      this.clearMsg();
      return;
    }

    this.api.updateFeedback(f._id, { replied: true, adminReply: reply }).subscribe({
      next: () => {
        f.replied = true;
        f.adminReply = reply;
        this.successMsg = `Đã gửi phản hồi đến khách hàng.`;
        this.clearMsg();
        this.applyFilter();
        this.closeDetail();
      },
      error: () => {
        this.errorMsg = 'Không thể cập nhật trạng thái. Vui lòng thử lại.';
        this.clearMsg();
      }
    });
  }

  // ── Detail modal ──
  openDetail(f: any) {
    this.selectedFeedback = f;
    this.adminReply = f.adminReply || '';
    this.showDetail = true;
  }

  closeDetail() {
    this.showDetail = false;
    this.selectedFeedback = null;
  }

  // ── Delete with confirmation ──
  confirmDelete(f: any) {
    this.deletingFeedback = f;
    this.showDeleteConfirm = true;
    this.showDetail = false;
  }

  cancelDelete() {
    this.showDeleteConfirm = false;
    this.deletingFeedback = null;
  }

  doDelete() {
    if (!this.deletingFeedback) return;
    const name = this.deletingFeedback.fullName;
    this.api.deleteFeedback(this.deletingFeedback._id).subscribe({
      next: () => {
        this.successMsg = `Đã xóa phản hồi của ${name}.`;
        this.clearMsg();
        this.showDeleteConfirm = false;
        this.deletingFeedback = null;
        this.loadFeedback();
      },
      error: () => {
        this.errorMsg = 'Không thể xóa phản hồi. Vui lòng thử lại.';
        this.clearMsg();
        this.showDeleteConfirm = false;
      }
    });
  }

  private clearMsg() {
    setTimeout(() => {
      this.successMsg = '';
      this.errorMsg = '';
    }, 3000);
  }
}
