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
