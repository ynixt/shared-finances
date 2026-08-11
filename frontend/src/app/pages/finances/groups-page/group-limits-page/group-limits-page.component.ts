import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { ProgressSpinner } from 'primeng/progressspinner';

import {
  GroupQuotaEntitlementDto,
  GroupWithRoleDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupEntitlementsStore } from '../../../../services/group-entitlements.store';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-group-limits-page',
  imports: [FinancesTitleBarComponent, ProgressSpinner, TranslatePipe],
  templateUrl: './group-limits-page.component.html',
  styleUrl: './group-limits-page.component.scss',
})
export class GroupLimitsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly groupService = inject(GroupService);
  private readonly entitlementsStore = inject(GroupEntitlementsStore);

  readonly groupId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly entitlements = this.entitlementsStore.entitlements(this.groupId);

  group: GroupWithRoleDto | null = null;
  loading = true;

  constructor() {
    if (this.groupId === '') {
      void this.router.navigateByUrl('/not-found');
      return;
    }
    void this.load();
  }

  percentage(item: GroupQuotaEntitlementDto): number {
    if (item.unlimited || item.limit == null || item.limit <= 0) return 0;
    return Math.min(100, Math.round((item.usage / item.limit) * 100));
  }

  remaining(item: GroupQuotaEntitlementDto): number {
    return item.limit == null ? 0 : Math.max(0, item.limit - item.usage);
  }

  private async load(): Promise<void> {
    try {
      [this.group] = await Promise.all([this.groupService.getGroup(this.groupId), this.entitlementsStore.load(this.groupId)]);
    } catch (error) {
      if (error instanceof HttpErrorResponse && (error.status === 404 || error.status === 400)) {
        await this.router.navigateByUrl('/not-found');
        return;
      }
      throw error;
    } finally {
      this.loading = false;
    }
  }
}
