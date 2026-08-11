import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-account-activity-info',
  imports: [DatePipe, TranslatePipe],
  templateUrl: './account-activity-info.component.html',
})
export class AccountActivityInfoComponent {
  readonly lastLoginAt = input.required<string>();
  readonly projectedDeletionAt = input<string | null>();
}
