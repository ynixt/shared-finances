import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { ProgressSpinner } from 'primeng/progressspinner';

import { PlanQuotaEntitlementDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { LocalDatePipe } from '../../../pipes/local-date.pipe';
import { PlanEntitlementsStore } from '../../../services/plan-entitlements.store';
import { UserService } from '../../../services/user.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';

@Component({
  selector: 'app-user-limits-page',
  imports: [FinancesTitleBarComponent, LocalDatePipe, ProgressSpinner, TranslatePipe],
  templateUrl: './user-limits-page.component.html',
  styleUrl: './user-limits-page.component.scss',
})
export class UserLimitsPageComponent {
  readonly entitlements = inject(PlanEntitlementsStore).entitlements;
  readonly user = inject(UserService).user;

  percentage(item: PlanQuotaEntitlementDto): number {
    if (item.unlimited || item.limit == null || item.limit <= 0) return 0;
    return Math.min(100, Math.round((item.usage / item.limit) * 100));
  }

  remaining(item: PlanQuotaEntitlementDto): number {
    return item.limit == null ? 0 : Math.max(0, item.limit - item.usage);
  }
}
