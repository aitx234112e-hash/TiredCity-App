import { PartialType } from '@nestjs/mapped-types';
import { CreateProductDto } from './create-product.dto';

// Tat ca field tro thanh optional khi update
export class UpdateProductDto extends PartialType(CreateProductDto) {}
