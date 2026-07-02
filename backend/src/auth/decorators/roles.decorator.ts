import { SetMetadata } from '@nestjs/common';
import { Role } from '@prisma/client';

export const ROLES_KEY = 'roles';

/** Gan role duoc phep truy cap route. Vd: @Roles(Role.ADMIN, Role.STAFF) */
export const Roles = (...roles: Role[]) => SetMetadata(ROLES_KEY, roles);
