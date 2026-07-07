import { Component, ElementRef, OnInit, ViewChild, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BlogApiService } from '../../blog-api.service';

export type BlogStatus = 'draft' | 'published';

export interface BlogItem {
  _id: string;
  title: string;
  slug?: string;
  excerpt?: string;
  content: string;
  thumbnail?: string;
  authorId?: string;
  authorName?: string;
  status: BlogStatus;
  publishedAt?: any;
  createdAt?: any;
  updatedAt?: any;
}

export interface BlogForm {
  title: string;
  excerpt: string;
  content: string;
  thumbnail: string;
  authorId: string;
  authorName: string;
  status: BlogStatus;
  publishedAt: string;
}

@Component({
  selector: 'app-blog-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './blog-management.html',
  styleUrl: './blog-management.css',
})
export class BlogManagement implements OnInit {

  @ViewChild('contentEditor') contentEditor?: ElementRef<HTMLDivElement>;
  @ViewChild('blogFormPanel') blogFormPanel?: ElementRef;

  blogs: BlogItem[] = [];
  filteredBlogs: BlogItem[] = [];
  paginatedBlogs: BlogItem[] = [];

  searchText = '';
  statusFilter = '';

  currentPage = 1;
  pageSize = 8;
  totalPages = 1;

  showForm = false;
  isEditing = false;
  editingId: string | null = null;

  editorFontSize = '3';

  loading = false;
  saving = false;
  errorMsg = '';
  successMsg = '';

  formErrors = { title: false, content: false };
  blogForm: BlogForm = this.createEmptyForm();

  constructor(
    private blogApi: BlogApiService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadBlogs();
    this.setDefaultAuthor();
  }

  loadBlogs(): void {
    this.loading = true;
    this.blogApi.getBlogs().subscribe({
      next: (data) => {
        this.zone.run(() => {
          this.blogs = (data as BlogItem[]).sort((a, b) => {
            const dateA = new Date(a.createdAt || 0).getTime();
            const dateB = new Date(b.createdAt || 0).getTime();
            return dateB - dateA;
          });
          this.applyFilters();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.zone.run(() => {
          this.loading = false;
          this.errorMsg = 'Lỗi tải danh sách blog.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  applyFilters(): void {
    const keyword = this.searchText.trim().toLowerCase();
    this.filteredBlogs = this.blogs.filter((blog) => {
      const matchKeyword = !keyword ||
        blog.title?.toLowerCase().includes(keyword) ||
        blog.excerpt?.toLowerCase().includes(keyword);
      const matchStatus = !this.statusFilter || blog.status === this.statusFilter;
      return matchKeyword && matchStatus;
    });
    this.currentPage = 1;
    this.totalPages = Math.max(1, Math.ceil(this.filteredBlogs.length / this.pageSize));
    this.updatePagination();
  }

  updatePagination(): void {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginatedBlogs = this.filteredBlogs.slice(start, start + this.pageSize);
  }

  previousPage() { if (this.currentPage > 1) { this.currentPage--; this.updatePagination(); } }
  nextPage() { if (this.currentPage < this.totalPages) { this.currentPage++; this.updatePagination(); } }

  openCreateForm(): void {
    this.isEditing = false;
    this.editingId = null;
    this.blogForm = this.createEmptyForm();
    this.setDefaultAuthor();
    this.showForm = true;
  }

  openEditForm(blog: BlogItem): void {
    this.isEditing = true;
    this.editingId = blog._id;
    this.blogForm = {
      title: blog.title || '',
      excerpt: blog.excerpt || '',
      content: blog.content || '',
      thumbnail: blog.thumbnail || '',
      authorId: String(blog.authorId || ''),
      authorName: blog.authorName || 'Admin',
      status: blog.status || 'draft',
      publishedAt: this.toDateTimeLocal(blog.publishedAt),
    };
    this.showForm = true;
    setTimeout(() => {
      if (this.contentEditor?.nativeElement) this.contentEditor.nativeElement.innerHTML = blog.content || '';
    }, 100);
  }

  closeForm(): void { this.showForm = false; }

  onThumbnailFileChange(event: any): void {
    const file = event.target.files?.[0];
    if (!file) return;
    this.blogApi.uploadImage(file).subscribe({
      next: (res) => { this.blogForm.thumbnail = res?.imageUrl || ''; this.cdr.detectChanges(); },
      error: () => this.errorMsg = 'Upload ảnh thất bại'
    });
  }

  onEditorInput(): void { this.blogForm.content = this.contentEditor?.nativeElement.innerHTML || ''; }
  formatText(command: string, value?: string): void { document.execCommand(command, false, value); this.onEditorInput(); }
  setFontSize(size: string): void { this.editorFontSize = size; this.formatText('fontSize', size); }

  onContentImageUpload(event: any): void {
    const file = event.target.files?.[0];
    if (!file) return;
    this.blogApi.uploadImage(file).subscribe({
      next: (res) => {
        const imageUrl = res?.imageUrl || '';
        if (!imageUrl) return;
        if (this.contentEditor?.nativeElement) {
            this.contentEditor.nativeElement.focus();
            document.execCommand('insertImage', false, imageUrl);
            this.onEditorInput();
        }
      }
    });
  }

  saveDraft() { this.submit('draft'); }
  publishBlog() { this.submit('published'); }

  submit(status: BlogStatus): void {
    if (this.contentEditor?.nativeElement) this.blogForm.content = this.contentEditor.nativeElement.innerHTML || '';
    if (!this.blogForm.title.trim() || !this.blogForm.content.trim()) {
      this.errorMsg = 'Vui lòng nhập đầy đủ tiêu đề và nội dung';
      return;
    }

    this.saving = true;
    const payload = {
      ...this.blogForm,
      title: this.blogForm.title.trim(),
      status,
      publishedAt: status === 'published' ? new Date().toISOString() : null,
    };

    const request$ = this.isEditing && this.editingId
      ? this.blogApi.updateBlog(this.editingId, payload)
      : this.blogApi.addBlog(payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.successMsg = 'Lưu bài viết thành công';
        setTimeout(() => { this.showForm = false; this.loadBlogs(); }, 1000);
      },
      error: () => { this.saving = false; this.errorMsg = 'Lỗi khi lưu bài viết'; }
    });
  }

  deleteBlog(id: string): void {
    if (!confirm('Xóa bài viết này?')) return;
    this.blogApi.deleteBlog(id).subscribe({ next: () => this.loadBlogs(), error: () => this.errorMsg = 'Lỗi khi xóa' });
  }

  toggleStatus(blog: BlogItem): void {
    const nextStatus: BlogStatus = blog.status === 'published' ? 'draft' : 'published';
    this.blogApi.updateBlog(blog._id, { status: nextStatus }).subscribe({ next: () => this.loadBlogs() });
  }

  statusLabel(status: BlogStatus): string { return status === 'published' ? 'Đã xuất bản' : 'Bản nháp'; }

  resolveThumbnail(src?: string): string {
    if (!src) return 'https://via.placeholder.com/56';
    return src;
  }

  trackByBlogId(_index: number, item: BlogItem): string { return item._id; }

  private setDefaultAuthor(): void {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      this.blogForm.authorId = user._id || '';
      this.blogForm.authorName = user.profileName || 'Admin';
    } catch {}
  }

  private createEmptyForm(): BlogForm {
    return { title: '', excerpt: '', content: '', thumbnail: '', authorId: '', authorName: 'Admin', status: 'draft', publishedAt: '' };
  }

  toDateTimeLocal(dateValue?: any): string {
    if (!dateValue) return '';
    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return '';
    return date.toISOString().slice(0, 16);
  }
}
