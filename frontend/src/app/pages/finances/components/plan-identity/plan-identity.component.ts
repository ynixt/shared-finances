import { Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-plan-identity',
  imports: [TranslatePipe],
  templateUrl: './plan-identity.component.html',
})
export class PlanIdentityComponent {
  readonly kind = input.required<'user' | 'group'>();
  readonly value = input.required<string>();

  protected readonly labelKey = computed(() => `planIdentity.${this.kind() === 'user' ? 'userPlan' : 'groupTier'}`);
  protected readonly valueKey = computed(() => `planIdentity.${this.kind() === 'user' ? 'userPlans' : 'groupTiers'}.${this.value()}`);
}
