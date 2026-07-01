import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BlogApiService } from '../../blog-api.service';

type BlogStatus = 'draft' | 'published';

interface BlogItem {
  _id: string;
  title: string;
  slug?: string;
  excerpt?: string;
  content: string;
  thumbnail?: string;
  authorId?: string;
  authorName?: string;
  status: BlogStatus;
  publishedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

interface BlogForm {
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

  formErrors = {
    title: false,
    content: false,
  };

  blogForm: BlogForm = this.createEmptyForm();

  constructor(private blogApi: BlogApiService) {}

  ngOnInit(): void {
    this.loadBlogs();
    this.setDefaultAuthor();
  }

  loadBlogs(): void {
    this.loading = true;
    this.errorMsg = '';

    this.blogApi.getBlogs().subscribe({
      next: (data) => {
        this.blogs = (data as BlogItem[]) || [];
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        console.error('Error loading blogs:', err);
        this.errorMsg = 'Không tải được danh sách blog từ Firestore';
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
    const end = start + this.pageSize;
    this.paginatedBlogs = this.filteredBlogs.slice(start, end);
  }

  previousPage(): void {
    if (this.currentPage <= 1) return;
    this.currentPage--;
    this.updatePagination();
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages) return;
    this.currentPage++;
    this.updatePagination();
  }

  openCreateForm(): void {
    this.isEditing = false;
    this.editingId = null;
    this.blogForm = this.createEmptyForm();
    this.setDefaultAuthor();
    this.formErrors = { title: false, content: false };
    this.showForm = true;
    setTimeout(() => {
      if (this.contentEditor?.nativeElement) {
        this.contentEditor.nativeElement.innerHTML = '';
      }
      this.blogFormPanel?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
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

    this.formErrors = { title: false, content: false };
    this.showForm = true;
    setTimeout(() => {
      if (this.contentEditor?.nativeElement) {
        this.contentEditor.nativeElement.innerHTML = this.blogForm.content || '';
      }
      this.blogFormPanel?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
  }

  closeForm(): void {
    this.showForm = false;
    this.errorMsg = '';
    this.successMsg = '';
  }

  onTitleChange(): void {
    // keep for title reactive binding
  }

  onThumbnailFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.blogApi.uploadImage(file).subscribe({
      next: (res) => {
        this.blogForm.thumbnail = res?.imageUrl || '';
      },
      error: () => {
        this.errorMsg = 'Upload ảnh thất bại';
      }
    });
  }

  onEditorInput(): void {
    this.blogForm.content = this.contentEditor?.nativeElement.innerHTML || '';
  }

  formatText(command: string, value?: string): void {
    this.focusEditor();
    document.execCommand(command, false, value);
    this.onEditorInput();
  }

  setFontSize(size: string): void {
    this.editorFontSize = size;
    this.formatText('fontSize', size);
  }

  onContentImageUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.blogApi.uploadImage(file).subscribe({
      next: (res) => {
        const imageUrl = res?.imageUrl || '';
        if (!imageUrl) return;
        this.focusEditor();
        document.execCommand('insertImage', false, imageUrl);
        this.onEditorInput();
      },
      error: () => {
        this.errorMsg = 'Upload ảnh trong nội dung thất bại';
      }
    });

    input.value = '';
  }

  saveDraft(): void {
    this.submit('draft');
  }

  publishBlog(): void {
    this.submit('published');
  }

  submit(status: BlogStatus): void {
    // Reset thông báo lỗi trước khi submit
    this.errorMsg = '';
    this.successMsg = '';

    // Đảm bảo luôn lấy nội dung mới nhất từ editor
    if (this.contentEditor?.nativeElement) {
      this.blogForm.content = this.contentEditor.nativeElement.innerHTML || '';
    }

    // Validation
    this.formErrors.title = !this.blogForm.title.trim();
    this.formErrors.content = !this.blogForm.content.trim() || this.blogForm.content === '<br>' || this.blogForm.content === '<div><br></div>';

    if (this.formErrors.title) {
      this.errorMsg = 'Vui lòng nhập tiêu đề bài viết';
      return;
    }

    if (this.formErrors.content) {
      this.errorMsg = 'Vui lòng nhập nội dung bài viết';
      return;
    }

    this.saving = true;
    const payload = {
      title: this.blogForm.title.trim(),
      excerpt: this.blogForm.excerpt.trim(),
      content: this.blogForm.content,
      thumbnail: this.blogForm.thumbnail,
      authorId: this.blogForm.authorId || undefined,
      authorName: this.blogForm.authorName || 'Admin',
      status,
      publishedAt: status === 'published'
        ? (this.blogForm.publishedAt ? new Date(this.blogForm.publishedAt) : new Date())
        : null,
    };

    const request$ = this.isEditing && this.editingId
      ? this.blogApi.updateBlog(this.editingId, payload)
      : this.blogApi.addBlog(payload);

    const successText = this.isEditing
      ? (status === 'published' ? 'Xuất bản bài viết thành công' : 'Cập nhật bài viết thành công')
      : (status === 'published' ? 'Xuất bản bài viết thành công' : 'Tạo bài viết thành công');

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.successMsg = successText;
        setTimeout(() => {
          this.showForm = false;
          this.loadBlogs();
        }, 1500);
      },
      error: (err) => {
        console.error('Blog submit error:', err);
        this.saving = false;
        this.errorMsg = 'Lưu bài viết vào Firestore thất bại. Kiểm tra cấu hình Firebase / quyền truy cập.';
      }
    });
  }

  deleteBlog(id: string): void {
    if (!confirm('Bạn có chắc muốn xóa bài viết này?')) return;

    this.blogApi.deleteBlog(id).subscribe({
      next: () => {
        this.successMsg = 'Đã xóa bài viết';
        this.loadBlogs();
      },
      error: () => {
        this.errorMsg = 'Xóa bài viết thất bại';
      }
    });
  }

  toggleStatus(blog: BlogItem): void {
    const nextStatus: BlogStatus = blog.status === 'published' ? 'draft' : 'published';
    const payload = {
      status: nextStatus,
      publishedAt: nextStatus === 'published' ? (blog.publishedAt || new Date()) : null
    };

    this.blogApi.updateBlog(blog._id, payload).subscribe({
      next: () => {
        this.loadBlogs();
      },
      error: () => {
        this.errorMsg = 'Đổi trạng thái thất bại';
      }
    });
  }

  visibleTags(tags: string[] = []): string[] {
    return tags.slice(0, 2);
  }

  remainingTagCount(tags: string[] = []): number {
    return Math.max(0, tags.length - 2);
  }

  statusLabel(status: BlogStatus): string {
    return status === 'published' ? 'Đã xuất bản' : 'Bản nháp';
  }

  resolveThumbnail(src?: string): string {
    if (!src) return 'https://via.placeholder.com/56';
    if (src.startsWith('http') || src.startsWith('data:') || src.startsWith('/')) return src;
    return `/assets/${src}`;
  }

  trackByBlogId(_index: number, item: BlogItem): string {
    return item._id;
  }

  private setDefaultAuthor(): void {
    let user = null;
    if (typeof window !== 'undefined' && typeof localStorage !== 'undefined') {
      const userRaw = localStorage.getItem('user');
      user = userRaw ? JSON.parse(userRaw) : null;
    }
    this.blogForm.authorId = user?._id || '';
    this.blogForm.authorName = user?.profileName || 'Admin';
  }

  private createEmptyForm(): BlogForm {
    return {
      title: '',
      excerpt: '',
      content: '',
      thumbnail: '',
      authorId: '',
      authorName: 'Admin',
      status: 'draft',
      publishedAt: '',
    };
  }

  private focusEditor(): void {
    this.contentEditor?.nativeElement?.focus();
  }

  toDateTimeLocal(dateValue?: string | null): string {
    if (!dateValue) return '';
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) return '';
    const offset = date.getTimezoneOffset();
    const local = new Date(date.getTime() - offset * 60000);
    return local.toISOString().slice(0, 16);
  }

}
