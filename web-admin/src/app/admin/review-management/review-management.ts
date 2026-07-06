import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReviewApiService } from '../../review-api.service';

@Component({
  selector: 'app-review-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './review-management.html',
  styleUrls: ['./review-management.css']
})
export class ReviewManagement implements OnInit {
  reviews: any[] = [];
  filteredReviews: any[] = [];
  searchText: string = '';
  loading = true;

  // Reply modal
  showReplyModal = false;
  selectedReview: any = null;
  replyContent: string = '';

  constructor(private reviewService: ReviewApiService) {}

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    this.reviewService.getReviews().subscribe({
      next: (data) => {
        this.reviews = data;
        this.applyFilter();
        this.loading = false;
      },
      error: (err) => {
        console.error('Lỗi tải đánh giá:', err);
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    const text = this.searchText.toLowerCase();
    this.filteredReviews = this.reviews.filter(r =>
      (r.userName || '').toLowerCase().includes(text) ||
      (r.comment || '').toLowerCase().includes(text) ||
      (r.productId || '').toLowerCase().includes(text)
    );
  }

  openReply(review: any): void {
    this.selectedReview = review;
    this.replyContent = review.adminReply || '';
    this.showReplyModal = true;
  }

  closeReply(): void {
    this.showReplyModal = false;
    this.selectedReview = null;
    this.replyContent = '';
  }

  submitReply(): void {
    if (!this.selectedReview) return;

    const data = {
      adminReply: this.replyContent,
      repliedAt: new Date(),
      status: 'APPROVED'
    };

    this.reviewService.updateReview(this.selectedReview._id, data).subscribe({
      next: () => {
        this.closeReply();
        // Updated via collectionData automatically
      },
      error: (err) => console.error('Lỗi gửi phản hồi:', err)
    });
  }

  deleteReview(id: string): void {
    if (confirm('Bạn có chắc muốn xóa đánh giá này?')) {
      this.reviewService.deleteReview(id).subscribe({
        next: () => {
          // Toast or message success
        },
        error: (err) => console.error('Lỗi xóa đánh giá:', err)
      });
    }
  }
}
