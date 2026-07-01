import { Inject, Injectable, BadRequestException } from '@nestjs/common';
import { v2 as Cloudinary, UploadApiResponse } from 'cloudinary';
import * as streamifier from 'streamifier';
import { CLOUDINARY } from './cloudinary.provider';

@Injectable()
export class CloudinaryService {
  constructor(@Inject(CLOUDINARY) private cloudinary: typeof Cloudinary) {}

  /** Upload buffer anh len thu muc tiredcity/products. */
  uploadImage(file: Express.Multer.File): Promise<UploadApiResponse> {
    if (!file?.buffer) throw new BadRequestException('Khong co file de upload');
    return new Promise((resolve, reject) => {
      const upload = this.cloudinary.uploader.upload_stream(
        { folder: 'tiredcity/products', resource_type: 'image' },
        (error, result) => {
          if (error) return reject(error);
          resolve(result);
        },
      );
      streamifier.createReadStream(file.buffer).pipe(upload);
    });
  }

  /** Xoa anh theo public_id (khi xoa san pham/anh). */
  async deleteImage(publicId: string): Promise<void> {
    if (!publicId) return;
    await this.cloudinary.uploader.destroy(publicId);
  }
}
