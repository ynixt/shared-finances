import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { faArrowTrendDown, faArrowTrendUp, faChartSimple, faClock, faDollarSign, faPencil } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';

import { BankAccountDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { EntrySummaryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { ONLY_DATE_FORMAT } from '../../../../util/date-util';
import { DashboardCardComponent } from '../../components/dashboard-card/dashboard-card.component';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { WalletEntryTableComponent } from '../../components/wallet-entry-table/wallet-entry-table.component';
import { BankAccountService } from '../../services/bank-account.service';
import { WalletEntryService } from '../../services/wallet-entry.service';

@Component({
  selector: 'app-view-bank-account-page',
  imports: [
    ProgressSpinner,
    FinancesTitleBarComponent,
    TranslatePipe,
    DashboardCardComponent,
    LocalCurrencyPipe,
    WalletEntryTableComponent,
    AdvancedDatePickerComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './view-bank-account-page.component.html',
  styleUrl: './view-bank-account-page.component.scss',
})
@UntilDestroy()
export class ViewBankAccountPageComponent {
  readonly balanceIcon = faDollarSign;
  readonly diffIcon = faChartSimple;
  readonly revenueIcon = faArrowTrendUp;
  readonly expensesIcon = faArrowTrendDown;
  readonly projectedIcon = faClock;

  readonly dateControl = new FormControl<DateRange | undefined>({
    startDate: dayjs().startOf('month'),
    endDate: dayjs().endOf('month'),
    sameMonth: true,
  });

  bankAccount: BankAccountDto | null = null;
  titleBarButtons: FinancesTitleBarExtraButton[] = [];
  dateRange: DateRange | undefined = undefined;
  summary: EntrySummaryDto | undefined = undefined;
  dateRangeOnlyOnPast = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bankAccountService: BankAccountService,
    private messageService: MessageService,
    private translateService: TranslateService,
    private errorMessageService: ErrorMessageService,
    private walletEntryService: WalletEntryService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getBankAccount(id);
      } else {
        this.goToNotFound();
      }
    });

    this.dateControl.valueChanges.pipe(untilDestroyed(this), startWith(this.dateControl.value)).subscribe(date => {
      this.dateRange = date == null ? undefined : date;
      this.dateRangeOnlyOnPast = this.dateRange?.endDate?.isBefore(dayjs()) ?? false;
      this.getSummary();
    });
  }

  private async getSummary() {
    if (this.bankAccount == null || this.dateRange == null) {
      this.summary = undefined;
      return;
    }

    this.summary = await this.walletEntryService.summaryWalletEntries({
      walletItemId: this.bankAccount.id!!,
      minimumDate: this.dateRange.startDate?.format(ONLY_DATE_FORMAT),
      maximumDate: this.dateRange.endDate?.format(ONLY_DATE_FORMAT),
      summaryType: 'BANK_ACCOUNT',
    });
  }

  private async getBankAccount(id: string): Promise<void> {
    try {
      this.bankAccount = await this.bankAccountService.getBankAccount(id);

      this.titleBarButtons = [
        {
          routerLink: `../edit/${id}`,
          rounded: true,
          tooltip: 'general.edit',
          icon: faPencil,
        },
      ];

      this.getSummary();
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 404 || error.status === 400) {
          await this.goToNotFound();
          return;
        }
      }

      this.errorMessageService.handleError(error, this.messageService);

      this.titleBarButtons = [];

      throw error;
    }
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }
}
