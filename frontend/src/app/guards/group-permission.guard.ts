import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';

import { GroupPermissions } from '../models/generated/com/ynixt/sharedfinances/domain/enums';
import { GroupService } from '../pages/finances/services/group.service';

type GroupPermissionGuardData = GroupPermissions | GroupPermissions[];

export const groupPermissionGuard: CanActivateFn = async route => {
  const groupService = inject(GroupService);
  const router = inject(Router);
  const requiredPermission = route.data['groupPermission'] as GroupPermissionGuardData | undefined;

  if (requiredPermission == null) {
    return true;
  }

  const groupId = resolveGroupId(route);

  if (groupId == null) {
    return router.createUrlTree(['/not-found']);
  }

  try {
    const group = await groupService.getGroup(groupId);
    const requiredPermissions = Array.isArray(requiredPermission) ? requiredPermission : [requiredPermission];
    const hasAllPermissions = requiredPermissions.every(permission => group.permissions.includes(permission));

    if (!hasAllPermissions) {
      return router.createUrlTree(['/not-found']);
    }

    return true;
  } catch (error) {
    if (error instanceof HttpErrorResponse && [400, 401, 403, 404].includes(error.status)) {
      return router.createUrlTree(['/not-found']);
    }

    throw error;
  }
};

function resolveGroupId(route: ActivatedRouteSnapshot): string | null {
  for (let i = route.pathFromRoot.length - 1; i >= 0; i -= 1) {
    const snapshot = route.pathFromRoot[i];
    const groupId = snapshot.paramMap.get('groupId') ?? snapshot.paramMap.get('id');

    if (groupId != null && groupId !== '') {
      return groupId;
    }
  }

  return null;
}
