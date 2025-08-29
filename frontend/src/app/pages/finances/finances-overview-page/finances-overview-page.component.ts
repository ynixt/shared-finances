import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';

@Component({
  selector: 'app-finances-overview-page',
  imports: [FinancesTitleBarComponent, TranslatePipe],
  templateUrl: './finances-overview-page.component.html',
  styleUrl: './finances-overview-page.component.scss',
})
export class FinancesOverviewPageComponent {}
