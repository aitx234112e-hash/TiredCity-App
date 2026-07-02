import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { UserApiService } from '../user-api.service';
import { AuditService } from '../admin/audit.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  loginForm: FormGroup;
  loginError: string = '';

  constructor(
    private fb: FormBuilder,
    private userApi: UserApiService,
    private router: Router,
    private audit: AuditService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {

    this.loginForm = this.fb.group({
      email: ['', [Validators.required]],
      password: ['', Validators.required],
      rememberMe: [false]
    });

  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      window.scrollTo({ top: 0, behavior: 'auto' });
    }
  }

  onSubmit() {
    if (this.loginForm.invalid) return;

    this.userApi.login(this.loginForm.value).subscribe(
      (res: any) => {
        // setUser đã được gọi trong tap của userApi.login()
        // Chỉ cho phép admin/superadmin đăng nhập vào trang quản trị
        if (res.disabled) {
          this.userApi.logout();
          this.loginError = 'Tài khoản đã bị vô hiệu hoá';
          alert(this.loginError);
          return;
        }
        if (res.role === 'admin' || res.role === 'superadmin') {
          this.audit.log('login', res.email || res._id, `Đăng nhập admin (${res.role})`);
          alert("Đăng nhập thành công");
          this.router.navigate(['/admin']);
        } else {
          this.userApi.logout();
          this.loginError = 'Tài khoản không có quyền truy cập trang quản trị';
          alert(this.loginError);
        }
      },
      (err: any) => {
        alert("Sai email hoặc mật khẩu");
      }
    );
  }

  goToForgotPassword(): void {
    this.router.navigate(['/forgot-password']);
  }
}
