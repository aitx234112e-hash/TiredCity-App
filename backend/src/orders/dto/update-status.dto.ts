import { ArrayMinSize, IsArray, IsEnum, IsOptional, IsString, IsUUID } from 'class-validator';
import { OrderStatus } from '@prisma/client';

export class UpdateStatusDto {
  @IsEnum(OrderStatus)
  status: OrderStatus;

  @IsOptional()
  @IsString()
  note?: string;
}

export class BulkUpdateStatusDto {
  @IsArray()
  @ArrayMinSize(1)
  @IsUUID('all', { each: true })
  orderIds: string[];

  @IsEnum(OrderStatus)
  status: OrderStatus;
}
