import { Component, Input } from '@angular/core';
import { BankAccountSummary } from 'src/app/@core/models';

@Component({
  selector: 'app-bank-account-summary-dashboard',
  templateUrl: './bank-account-summary-dashboard.component.html',
  styleUrls: ['./bank-account-summary-dashboard.component.scss'],
})
export class BankAccountSummaryDashboardComponent {
  @Input() bankAccountSummary: BankAccountSummary;
  @Input() isLoading = false;
}
