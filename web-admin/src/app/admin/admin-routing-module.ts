import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Mainpage } from './mainpage/mainpage';
import { Admin } from './admin/admin';
import { OrderManagement } from './order-management/order-management';
import { UserManagement } from './user-management/user-management';
import { ProductManagement } from './product-management/product-management';
import { BlogManagement } from './blog-management/blog-management';
import { FeedbackManagement } from './feedback-management/feedback-management';
import { Revenue } from './revenue/revenue';
import { Events } from './events/events';
import { Vouchers } from './vouchers/vouchers';
import { Shipping } from './shipping/shipping';
import { AuditLogs } from './audit-logs/audit-logs';
import { Reports } from './reports/reports';
import { Chatbot } from './chatbot/chatbot';
import { ReviewManagement } from './review-management/review-management';
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
      { path: 'reviews', component: ReviewManagement },
      { path: 'revenue', component: Revenue },
      { path: 'events', component: Events },
      { path: 'vouchers', component: Vouchers },
      { path: 'shipping', component: Shipping },
      { path: 'reports', component: Reports },
      { path: 'audit-logs', component: AuditLogs },
      { path: 'chatbot', component: Chatbot },

      { path: '', redirectTo: 'mainpage', pathMatch: 'full' }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
