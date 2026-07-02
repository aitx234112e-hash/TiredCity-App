import { IsEnum, IsInt, IsNotEmpty, IsNumber, IsOptional, IsString, Min } from 'class-validator';
import { Size } from '@prisma/client';

export class ProductVariantDto {
  @IsOptional()
  @IsString()
  id?: string; // co id => update, khong co => tao moi

  @IsNotEmpty()
  @IsString()
  color: string;

  @IsEnum(Size)
  size: Size;

  @IsNotEmpty()
  @IsString()
  sku: string;

  @IsNumber()
  @Min(0)
  price: number;

  @IsInt()
  @Min(0)
  stockQty: number;
}
