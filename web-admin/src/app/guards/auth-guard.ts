import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  // Trên server (SSR) không có localStorage — cho phép render, client sẽ kiểm tra lại quyền
  if (typeof window === 'undefined') return true;

  const raw = localStorage.getItem('user');
  if (!raw) {
    router.navigate(['/login']);
    return false;
  }

  try {
    const user = JSON.parse(raw as string);
    if (user && (user.role === 'admin' || user.role === 'superadmin')) {
      return true;
    }
  } catch (e) {
    // fallthrough
  }

  router.navigate(['/login']);
  return false;
};
