import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

/** Bao ve route: yeu cau co JWT hop le. */
@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {}
