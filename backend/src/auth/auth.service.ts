import { Injectable } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { Role, User } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';
import { GoogleProfile } from './strategies/google.strategy';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwt: JwtService,
    private config: ConfigService,
  ) {}

  /**
   * Tim hoac tao user tu profile Google.
   * - Lan dau dang nhap -> tao user role CUSTOMER (tru email seed admin).
   * - Da co -> cap nhat thong tin moi nhat.
   */
  async validateGoogleUser(profile: GoogleProfile): Promise<User> {
    const seedAdminEmail = this.config.get<string>('SEED_ADMIN_EMAIL');
    const existing = await this.prisma.user.findUnique({ where: { email: profile.email } });

    if (existing) {
      return this.prisma.user.update({
        where: { id: existing.id },
        data: {
          googleId: profile.googleId,
          fullName: profile.fullName || existing.fullName,
          avatarUrl: profile.avatarUrl ?? existing.avatarUrl,
        },
      });
    }

    return this.prisma.user.create({
      data: {
        googleId: profile.googleId,
        email: profile.email,
        fullName: profile.fullName,
        avatarUrl: profile.avatarUrl,
        role: profile.email === seedAdminEmail ? Role.ADMIN : Role.CUSTOMER,
      },
    });
  }

  /** Ky JWT chua id/email/role. */
  signToken(user: Pick<User, 'id' | 'email' | 'role'>): string {
    return this.jwt.sign({ sub: user.id, email: user.email, role: user.role });
  }

  /** Tra ho so user hien tai (cho endpoint /auth/me). */
  async getProfile(userId: string) {
    return this.prisma.user.findUnique({
      where: { id: userId },
      select: { id: true, email: true, fullName: true, avatarUrl: true, role: true, menh: true, isActive: true },
    });
  }
}
