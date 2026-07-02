import { Controller, Get, Post, Req, Res, UseGuards, ForbiddenException } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ConfigService } from '@nestjs/config';
import { Request, Response } from 'express';
import { Role } from '@prisma/client';
import { AuthService } from './auth.service';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { CurrentUser, JwtUser } from './decorators/current-user.decorator';
import { GoogleProfile } from './strategies/google.strategy';

@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private config: ConfigService,
  ) {}

  // 1. Diem khoi dong -> Passport redirect sang trang consent cua Google
  @Get('google')
  @UseGuards(AuthGuard('google'))
  googleLogin() {
    // Passport tu redirect, khong can body
  }

  // 2. Google goi lai voi profile -> tao user, ky JWT, set cookie, redirect ve web
  @Get('google/callback')
  @UseGuards(AuthGuard('google'))
  async googleCallback(@Req() req: Request, @Res() res: Response) {
    const profile = req.user as GoogleProfile;
    const user = await this.authService.validateGoogleUser(profile);

    // RBAC ngay tu cong dang nhap admin: chi STAFF/ADMIN/AUDITOR moi vao back-office
    const webUrl = this.config.get<string>('ADMIN_WEB_URL');
    const allowed: Role[] = [Role.ADMIN, Role.STAFF, Role.AUDITOR];
    if (!allowed.includes(user.role)) {
      return res.redirect(`${webUrl}/login?error=forbidden`);
    }
    if (!user.isActive) {
      return res.redirect(`${webUrl}/login?error=disabled`);
    }

    const token = this.authService.signToken(user);
    res.cookie('access_token', token, {
      httpOnly: true,
      secure: this.config.get('NODE_ENV') === 'production',
      sameSite: 'lax',
      maxAge: 7 * 24 * 60 * 60 * 1000, // 7 ngay
    });
    return res.redirect(`${webUrl}/dashboard`);
  }

  // 3. Ho so user dang dang nhap
  @Get('me')
  @UseGuards(JwtAuthGuard)
  me(@CurrentUser('id') userId: string) {
    return this.authService.getProfile(userId);
  }

  // 4. Dang xuat -> xoa cookie
  @Post('logout')
  @UseGuards(JwtAuthGuard)
  logout(@Res({ passthrough: true }) res: Response) {
    res.clearCookie('access_token');
    return { message: 'Da dang xuat' };
  }
}
