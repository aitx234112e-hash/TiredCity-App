import { Controller, Get, Patch, Post, Param, Body, Query, UseGuards, ParseUUIDPipe } from '@nestjs/common';
import { Role } from '@prisma/client';
import { OrdersService } from './orders.service';
import { QueryOrderDto } from './dto/query-order.dto';
import { BulkUpdateStatusDto, UpdateStatusDto } from './dto/update-status.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';

@Controller('orders')
@UseGuards(JwtAuthGuard, RolesGuard)
export class OrdersController {
  constructor(private readonly orders: OrdersService) {}

  @Get()
  findAll(@Query() query: QueryOrderDto) {
    return this.orders.findAll(query);
  }

  @Get(':id')
  findOne(@Param('id', ParseUUIDPipe) id: string) {
    return this.orders.findOne(id);
  }

  @Patch(':id/status')
  @Roles(Role.ADMIN, Role.STAFF)
  updateStatus(@Param('id', ParseUUIDPipe) id: string, @Body() dto: UpdateStatusDto) {
    return this.orders.updateStatus(id, dto);
  }

  @Post('bulk-status')
  @Roles(Role.ADMIN, Role.STAFF)
  bulkUpdate(@Body() dto: BulkUpdateStatusDto) {
    return this.orders.bulkUpdateStatus(dto);
  }
}
