import { Component, OnInit, ViewChild, ElementRef, NgZone, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProductApiService } from '../../product-api.service';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './product-management.html',
  styleUrls: ['./product-management.css'],
})
export class ProductManagement implements OnInit {

  searchProduct: string = '';
  filterCategory: string = '';
  filteredProducts: any[] = [];
  products: any[] = [];
  paginatedProducts: any[] = [];
  selectedProducts: string[] = [];

  productForm!: FormGroup;
  images: string[] = [];
  imageFileNames: string[] = [];
  uploadingImageAt: number | null = null;
  isEditing = false;
  editingProductId: string | null = null;
  sizesInput = [ { size: 'S', quantity: 0 }, { size: 'M', quantity: 0 }, { size: 'L', quantity: 0 }, { size: 'XL', quantity: 0 } ];

  currentPage = 1;
  pageSize = 50;
  totalPages = 1;

  canEdit = true;
  showForm = false;
  loading = false; // Thêm lại property loading
  successMsg = '';
  errorMsg = '';
  isSyncing = false;

  @ViewChild('formSection') formSection!: ElementRef;

  constructor(
    private fb: FormBuilder,
    private productService: ProductApiService,
    private zone: NgZone,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.productForm = this.fb.group({
      product_name: ['', Validators.required],
      product_dept: ['', Validators.required],
      color: [''],
      description: [''],
      unit_price: [0, [Validators.required, Validators.min(0)]],
      stock: [0, [Validators.min(0)]],
      discount: [0],
      rating: [4],
      material: [''],
      origin: [''],
      quantityS: [0], quantityM: [0], quantityL: [0], quantityXL: [0]
    });
  }

  loadProducts() {
    this.loading = true;
    this.productService.getProducts().subscribe({
      next: (data: any[]) => {
        this.zone.run(() => {
          this.products = data;
          this.applyProductFilter();
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // Khôi phục hàm filter bị mất
  applyProductFilter() {
    const text = this.searchProduct.toLowerCase();
    this.filteredProducts = this.products.filter(p => {
      const matchText = (p.product_name || '').toLowerCase().includes(text);
      const matchCat = this.filterCategory ? p.product_dept === this.filterCategory : true;
      return matchText && matchCat;
    });
    this.totalPages = Math.ceil(this.filteredProducts.length / this.pageSize) || 1;
    this.updatePagination();
  }

  updatePagination() {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginatedProducts = this.filteredProducts.slice(start, start + this.pageSize);
  }

  previousPage() { if (this.currentPage > 1) { this.currentPage--; this.updatePagination(); } }
  nextPage() { if (this.currentPage < this.totalPages) { this.currentPage++; this.updatePagination(); } }

  toggleForm() { this.showForm = !this.showForm; if (!this.showForm) this.cancelEdit(); }
  showSuccess(msg: string) { this.successMsg = msg; setTimeout(() => this.successMsg = '', 3000); }
  showError(msg: string) { this.errorMsg = msg; setTimeout(() => this.errorMsg = '', 6000); }

  createProduct() {
    if (this.productForm.invalid) return;
    const data = { ...this.productForm.value, images: this.images.filter(i => i) };
    this.productService.addProduct(data).subscribe(() => {
        this.showSuccess('Thêm thành công');
        this.showForm = false;
    });
  }

  editProduct(product: any) {
    this.isEditing = true;
    this.editingProductId = product._id;
    this.productForm.patchValue(product);
    this.images = product.images || [];
    this.showForm = true;
  }

  updateProduct() {
    if (!this.editingProductId) return;
    this.productService.updateProduct(this.editingProductId, this.productForm.value).subscribe(() => {
        this.showSuccess('Cập nhật thành công');
        this.cancelEdit();
    });
  }

  cancelEdit() {
    this.isEditing = false;
    this.editingProductId = null;
    this.productForm.reset();
    this.images = [];
    this.showForm = false;
  }

  deleteProduct(id: string) {
    if (confirm('Xóa sản phẩm này?')) this.productService.deleteProduct(id).subscribe();
  }

  deleteSelectedProducts() {
    if (this.selectedProducts.length === 0) return;
    if (confirm(`Xóa ${this.selectedProducts.length} sản phẩm đã chọn?`)) {
      this.selectedProducts.forEach(id => this.productService.deleteProduct(id).subscribe());
      this.selectedProducts = [];
    }
  }

  isSelected(id: string) { return this.selectedProducts.includes(id); }
  toggleSelect(id: string) {
    if (this.isSelected(id)) this.selectedProducts = this.selectedProducts.filter(x => x !== id);
    else this.selectedProducts.push(id);
  }
  toggleSelectAll(event: any) {
    this.selectedProducts = event.target.checked ? this.paginatedProducts.map(p => p._id) : [];
  }

  onImageChange(event: any, index: number) {
    const file = event.target.files[0];
    if (!file) return;
    this.productService.uploadImage(file).subscribe(res => {
      this.images[index] = res.imageUrl;
      this.cdr.detectChanges();
    });
  }

  fixInconsistentStock() { /* logic giữ nguyên */ }
  clearImage(index: number) { this.images[index] = ''; }
}
