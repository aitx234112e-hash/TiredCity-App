import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { FirebaseService } from '../firebase.service';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class FirebaseAuthGuard implements CanActivate {
  constructor(
    private firebaseService: FirebaseService,
    private prisma: PrismaService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest();
    const authHeader = request.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing or invalid Authorization header');
    }

    const token = authHeader.split(' ')[1];
    try {
      const decodedToken = await this.firebaseService.getAuth().verifyIdToken(token);
      
      // Look up DB user
      const user = await this.prisma.user.findUnique({
        where: { email: decodedToken.email },
      });
      
      request.firebaseUser = decodedToken; // Luôn set firebaseUser
      if (user) {
        request.user = user; // Set DB user nếu có
      } else {
        // Nếu không có trong DB, vẫn cho đi tiếp nếu là API /sync, 
        // nhưng với các API khác thì throw lỗi hoặc controller tự xử lý?
        // Wait, Guard cho phép đi tiếp nhưng controller sẽ check. 
        // Tốt nhất là throw lỗi nếu không phải là API /sync.
        const isSyncRoute = request.url.includes('/auth/sync');
        if (!isSyncRoute) {
          throw new UnauthorizedException('User not found in DB. Please sync first.');
        }
      }
      return true;
    } catch (error) {
      throw new UnauthorizedException('Invalid Firebase Token: ' + error.message);
    }
  }
}
