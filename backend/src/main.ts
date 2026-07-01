import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as cookieParser from 'cookie-parser';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const config = app.get(ConfigService);

  app.setGlobalPrefix('api');
  app.use(cookieParser());

  // Validation toan cuc cho moi DTO
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,            // loai bo field thua
      forbidNonWhitelisted: true, // bao loi neu gui field la
      transform: true,            // tu dong ep kieu theo DTO
      transformOptions: { enableImplicitConversion: true },
    }),
  );

  // CORS cho Admin web (gui cookie kem theo)
  app.enableCors({
    origin: config.get<string>('ADMIN_WEB_URL') ?? 'http://localhost:3000',
    credentials: true,
  });

  const port = config.get<number>('PORT') ?? 4000;
  await app.listen(port);
  console.log(`🚀 TiredCity Admin API: http://localhost:${port}/api`);
}
bootstrap();
