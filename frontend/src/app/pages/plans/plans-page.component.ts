import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faGithub } from '@fortawesome/free-brands-svg-icons/faGithub';
import { TranslatePipe } from '@ngx-translate/core';

import { firstValueFrom } from 'rxjs';

import { ButtonDirective, ButtonLabel } from 'primeng/button';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import {
  PublishedPlanComparisonDto,
  PublishedPlanLimitDto,
} from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/plan';
import { PlanLimitKey } from '../../models/generated/com/ynixt/sharedfinances/domain/enums';

@Component({
  selector: 'app-plans-page',
  imports: [NavbarComponent, TranslatePipe, FaIconComponent, RouterLink, ButtonDirective, ButtonLabel],
  templateUrl: './plans-page.component.html',
  styleUrl: './plans-page.component.scss',
})
export class PlansPageComponent implements OnInit {
  private readonly http = inject(HttpClient);

  comparison: PublishedPlanComparisonDto | null = null;
  loadFailed = false;

  async ngOnInit(): Promise<void> {
    try {
      const comparison = await firstValueFrom(this.http.get<PublishedPlanComparisonDto>('/api/open/plans'));
      this.comparison = {
        ...comparison,
        userPlans: comparison.userPlans.filter(plan => plan.plan !== 'ADMINISTRATOR'),
      };
    } catch {
      this.loadFailed = true;
    }
  }

  userQuotaKeys(comparison: PublishedPlanComparisonDto): PlanLimitKey[] {
    return comparison.userPlans[0]?.limits.map(item => item.quota) ?? [];
  }

  groupQuotaKeys(comparison: PublishedPlanComparisonDto): PlanLimitKey[] {
    return comparison.groupTiers[0]?.limits.map(item => item.quota) ?? [];
  }

  limit(limits: PublishedPlanLimitDto[], quota: PlanLimitKey): PublishedPlanLimitDto | undefined {
    return limits.find(item => item.quota === quota);
  }

  protected readonly faGithub = faGithub;
}
