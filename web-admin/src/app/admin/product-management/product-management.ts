import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
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

  applyProductFilter() {
    const text = this.searchProduct.toLowerCase();
    this.filteredProducts = this.products.filter(p => {
      const matchText = p.product_name.toLowerCase().includes(text);
      const matchCat = this.filterCategory ? p.product_dept === this.filterCategory : true;
      return matchText && matchCat;
    });
    this.totalPages = Math.ceil(this.filteredProducts.length / this.pageSize) || 1;
    this.currentPage = 1;
    this.updatePagination();
  }

  products: any[] = [];
  paginatedProducts: any[] = [];
  selectedProducts: string[] = [];

  productForm!: FormGroup;
  images: string[] = [];
  imageFileNames: string[] = [];
  uploadingImageAt: number | null = null;
  isEditing = false;
  editingProductId: string | null = null;
  sizesInput = [
    { size: 'S', quantity: 0 },
    { size: 'M', quantity: 0 },
    { size: 'L', quantity: 0 },
    { size: 'XL', quantity: 0 }
  ];

  currentPage = 1;
  pageSize = 50;
  totalPages = 1;

  canEdit = true;
  showForm = false;
  successMsg = '';
  errorMsg = '';

  @ViewChild('formSection') formSection!: ElementRef;

  constructor(private fb: FormBuilder, private productService: ProductApiService) {}

  ngOnInit(): void {
    this.loadProducts();
    this.productForm = this.fb.group({
      product_name: ['', Validators.required],
      product_dept: ['', Validators.required],
      color: [''],
      description: [''],
      unit_price: [0, [Validators.required, Validators.min(0)]],
      // stock used for accessories
      stock: [0, [Validators.min(0)]],
      discount: [0],
      rating: [4],
      material: [''],
      origin: [''],
      suitableFor: [''],
      quantityS: [0, [Validators.required, Validators.min(0)]],
      quantityM: [0, [Validators.required, Validators.min(0)]],
      quantityL: [0, [Validators.required, Validators.min(0)]],
      quantityXL: [0, [Validators.required, Validators.min(0)]],
    });

    // react to category changes to toggle validators
    this.productForm.get('product_dept')?.valueChanges.subscribe((v) => {
      if (v === 'phu-kien') {
        // accessories: make stock required, remove size validators
        this.productForm.get('stock')?.setValidators([Validators.required, Validators.min(0)]);
        ['quantityS','quantityM','quantityL','quantityXL'].forEach(k => {
          this.productForm.get(k)?.clearValidators();
          this.productForm.get(k)?.updateValueAndValidity();
        });
      } else {
        // clothing: require at least one size (validators on inputs)
        this.productForm.get('stock')?.clearValidators();
        this.productForm.get('stock')?.updateValueAndValidity();
        ['quantityS','quantityM','quantityL','quantityXL'].forEach(k => {
          this.productForm.get(k)?.setValidators([Validators.required, Validators.min(0)]);
          this.productForm.get(k)?.updateValueAndValidity();
        });
      }
      this.productForm.get('stock')?.updateValueAndValidity();
    });
  }

  loadProducts() {
    this.productService.getProducts().subscribe({
      next: (data: any[]) => {
        this.products = data;
        this.filteredProducts = data;
        this.totalPages = Math.ceil(this.products.length / this.pageSize) || 1;
        this.currentPage = 1;
        this.updatePagination();
      },
      error: () => this.showError('Không thể tải danh sách sản phẩm!')
    });
  }

  updatePagination() {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginatedProducts = this.filteredProducts.slice(start, start + this.pageSize);
  }

  previousPage() {
    if (this.currentPage > 1) { this.currentPage--; this.updatePagination(); }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) { this.currentPage++; this.updatePagination(); }
  }

  toggleForm() {
    this.showForm = !this.showForm;
    if (!this.showForm) {
      this.cancelEdit();
    } else {
      setTimeout(() => {
        this.formSection?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 50);
    }
  }

  showSuccess(msg: string) {
    this.successMsg = msg;
    this.errorMsg = '';
    setTimeout(() => this.successMsg = '', 3000);
  }

  showError(msg: string) {
    this.errorMsg = msg;
    this.successMsg = '';
    setTimeout(() => this.errorMsg = '', 6000);
  }

  // =====================
  // THÊM MỚI
  // =====================
  createProduct() {
    if (this.uploadingImageAt !== null) {
      this.showError('Vui lòng chờ tải ảnh xong rồi thêm sản phẩm.');
      return;
    }

    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      this.showError('❌ Vui lòng điền đầy đủ: Tên sản phẩm và Danh mục.');
      return;
    }
    const formVal = this.productForm.value;
    let data: any = { ...formVal, images: this.images.filter(img => img) };
    if (formVal.product_dept === 'phu-kien') {
      // accessories: use stock
      data.sizes = [];
      data.stock = Number(formVal.stock) || 0;
    } else {
      // clothing: build sizes array
      this.sizesInput = [
        { size: 'S', quantity: formVal.quantityS },
        { size: 'M', quantity: formVal.quantityM },
        { size: 'L', quantity: formVal.quantityL },
        { size: 'XL', quantity: formVal.quantityXL }
      ];
      const sizes = this.sizesInput.filter(s => typeof s.quantity === 'number' && s.quantity > 0);
      data.sizes = sizes;
      data.stock = 0;
    }
    this.productService.addProduct(data).subscribe({
      next: (res: any) => {
        this.products.unshift(res); // thêm vào đầu danh sách
        this.totalPages = Math.ceil(this.products.length / this.pageSize) || 1;
        this.updatePagination();
        this.productForm.reset({ unit_price: 0, stocked_quantity: 0, discount: 0, rating: 4 });
        this.images = [];
        this.imageFileNames = [];
        this.showSuccess('Thêm sản phẩm thành công!');
      },
      error: (err: any) => {
        const msg = err?.error?.error || err?.error?.message || 'Lỗi khi thêm sản phẩm!';
        this.showError(`❌ ${msg}`);
      }
    });
  }

  // =====================
  // CHỈNH SỬa
  // =====================
  editProduct(product: any) {
    this.isEditing = true;
    this.editingProductId = product._id;
    this.productForm.patchValue({
      product_name: product.product_name || '',
      product_dept: product.product_dept || '',
      color: product.color || '',
      short_description: product.short_description || '',
      description: product.description || '',
      unit_price: product.unit_price || 0,
      discount: product.discount || 0,
      rating: product.rating || 4,
      material: product.material || '',
      origin: product.origin || '',
    });
    // Gán lại size
    this.sizesInput = [
      { size: 'S', quantity: 0 },
      { size: 'M', quantity: 0 },
      { size: 'L', quantity: 0 },
      { size: 'XL', quantity: 0 }
    ];
    if (Array.isArray(product.sizes)) {
      for (const s of product.sizes) {
        const idx = this.sizesInput.findIndex(x => x.size === s.size);
        if (idx !== -1) this.sizesInput[idx].quantity = s.quantity;
      }
    }
    this.images = product.images ? [...product.images] : [];
    this.imageFileNames = this.images.map((img) => {
      const last = img.split('/').pop() || img;
      return last.split('?')[0];
    });
    this.showForm = true;
    setTimeout(() => {
      this.formSection?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 50);
  }

  updateProduct() {
    if (this.uploadingImageAt !== null) {
      this.showError('Vui lòng chờ tải ảnh xong rồi cập nhật sản phẩm.');
      return;
    }

    if (!this.editingProductId || this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      this.showError('❌ Vui lòng điền đầy đủ: Tên sản phẩm và Danh mục.');
      return;
    }
    const formVal = this.productForm.value;
    let data: any = { ...formVal, images: this.images.filter(img => img) };
    if (formVal.product_dept === 'phu-kien') {
      data.sizes = [];
      data.stock = Number(formVal.stock) || 0;
    } else {
      const sizes = this.sizesInput.filter(s => typeof s.quantity === 'number' && s.quantity > 0);
      if (sizes.length === 0) {
        this.showError('Vui lòng nhập số lượng cho ít nhất 1 size.');
        return;
      }
      data.sizes = sizes;
      data.stock = 0;
    }
    this.productService.updateProduct(this.editingProductId, data).subscribe({
      next: (res: any) => {
        const idx = this.products.findIndex(p => p._id === this.editingProductId);
        if (idx !== -1) this.products[idx] = res;
        this.cancelEdit();
        this.updatePagination();
        this.showSuccess('✅ Cập nhật sản phẩm thành công!');
      },
      error: (err: any) => {
        const msg = err?.error?.error || err?.error?.message || 'Lỗi khi cập nhật sản phẩm!';
        this.showError(`❌ ${msg}`);
      }
    });
  }

  cancelEdit() {
    this.isEditing = false;
    this.editingProductId = null;
    this.productForm.reset({ unit_price: 0, discount: 0, rating: 4 });
    this.images = [];
    this.imageFileNames = [];
    this.uploadingImageAt = null;
    this.sizesInput = [
      { size: 'S', quantity: 0 },
      { size: 'M', quantity: 0 },
      { size: 'L', quantity: 0 },
      { size: 'XL', quantity: 0 }
    ];
  }

  // =====================
  // XÓA
  // =====================
  deleteProduct(id: string) {
    if (!confirm('Bạn có chắc muốn xóa sản phẩm này?')) return;
    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p._id !== id);
        this.selectedProducts = this.selectedProducts.filter(x => x !== id);
        this.totalPages = Math.ceil(this.products.length / this.pageSize) || 1;
        if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
        this.updatePagination();
        this.showSuccess('✅ Xóa sản phẩm thành công!');
      },
      error: () => this.showError('❌ Lỗi khi xóa sản phẩm!')
    });
  }

  deleteSelectedProducts() {
    if (this.selectedProducts.length === 0) return;
    if (!confirm(`Bạn có chắc muốn xóa ${this.selectedProducts.length} sản phẩm đã chọn?`)) return;

    const ids = [...this.selectedProducts];
    let completed = 0;
    let hasError = false;

    ids.forEach(id => {
      this.productService.deleteProduct(id).subscribe({
        next: () => {
          this.products = this.products.filter(p => p._id !== id);
          completed++;
          if (completed === ids.length) {
            this.selectedProducts = [];
            this.totalPages = Math.ceil(this.products.length / this.pageSize) || 1;
            if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;
            this.updatePagination();
            if (!hasError) this.showSuccess(`✅ Đã xóa ${ids.length} sản phẩm thành công!`);
          }
        },
        error: () => {
          hasError = true;
          this.showError('❌ Lỗi khi xóa một số sản phẩm!');
        }
      });
    });
  }

  isSelected(id: string) { return this.selectedProducts.includes(id); }

  toggleSelect(id: string) {
    if (this.selectedProducts.includes(id))
      this.selectedProducts = this.selectedProducts.filter(x => x !== id);
    else
      this.selectedProducts.push(id);
  }

  toggleSelectAll(event: any) {
    if (event.target.checked)
      this.selectedProducts = this.paginatedProducts.map(p => p._id);
    else
      this.selectedProducts = [];
  }

  onImageChange(event: any, index: number) {
    const file = event.target.files[0];
    if (!file) return;

    this.uploadingImageAt = index;
    this.productService.uploadImage(file).subscribe({
      next: (res) => {
        this.images[index] = res.imageUrl;
        this.imageFileNames[index] = res.fileName;
        this.uploadingImageAt = null;
        this.showSuccess(`Đã tải ảnh ${res.fileName}`);
      },
      error: (err: any) => {
        const msg = err?.error?.message || 'Upload ảnh thất bại!';
        this.uploadingImageAt = null;
        this.showError(`❌ ${msg}`);
      }
    });
  }

  clearImage(index: number) {
    this.images[index] = '';
    this.imageFileNames[index] = '';
  }
}
