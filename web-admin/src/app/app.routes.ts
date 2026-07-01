import { Routes } from '@angular/router';
import { Login } from './login/login';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: 'admin',
    loadChildren: () => import('../app/admin/admin-module').then(m => m.AdminModule)
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' },
];
