import { Controller, Get, Query, UseGuards, ParseIntPipe, DefaultValuePipe } from '@nestjs/common';
import { Role } from '@prisma/client';
import { DashboardService } from './dashboard.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('dashboard')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles(Role.ADMIN, Role.STAFF, Role.AUDITOR)
export class DashboardController {
  constructor(private readonly dashboard: DashboardService) {}

  @Get('overview')
  overview() {
    return this.dashboard.overview();
  }

  @Get('revenue')
  revenue(@Query('days', new DefaultValuePipe(14), ParseIntPipe) days: number) {
    return this.dashboard.revenueByDay(days);
  }

  @Get('orders-by-status')
  ordersByStatus() {
    return this.dashboard.ordersByStatus();
  }

  @Get('top-products')
  topProducts(@Query('limit', new DefaultValuePipe(5), ParseIntPipe) limit: number) {
    return this.dashboard.topProducts(limit);
  }
}
