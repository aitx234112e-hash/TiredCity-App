import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Mainpage } from './mainpage/mainpage';
import { Admin } from './admin/admin';
import { OrderManagement } from './order-management/order-management';
import { UserManagement } from './user-management/user-management';
import { ProductManagement } from './product-management/product-management';
import { BlogManagement } from './blog-management/blog-management';
import { FeedbackManagement } from './feedback-management/feedback-management';
import { authGuard } from '../guards/auth-guard';

const routes: Routes = [
  {
    path: '',
    component: Admin,
    canActivate: [authGuard],
    children: [
      { path: 'mainpage', component: Mainpage },
      { path: 'orders', component: OrderManagement },
      { path: 'users', component: UserManagement },
      { path: 'products', component: ProductManagement },
      { path: 'blogs', component: BlogManagement },
      { path: 'feedbacks', component: FeedbackManagement },

      { path: '', redirectTo: 'mainpage', pathMatch: 'full' }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
