import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { faCalendarMinus, faCalendarXmark, faDollarSign, faPencil } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ProgressSpinner } from 'primeng/progressspinner';

import {
  CreditCardBillDto,
  CreditCardDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { DashboardCardComponent } from '../../components/dashboard-card/dashboard-card.component';
import { FinancesTitleBarComponent, FinancesTitleBarExtraButton } from '../../components/finances-title-bar/finances-title-bar.component';
import {
  AdvancedDatePickerComponent,
  DateRange,
} from '../../components/wallet-entry-table/components/advanced-date-picker/advanced-date-picker.component';
import { WalletEntryTableComponent } from '../../components/wallet-entry-table/wallet-entry-table.component';
import { CreditCardBillService } from '../../services/credit-card-bill.service';
import { CreditCardService } from '../../services/credit-card.service';
import { WalletEntryService } from '../../services/wallet-entry.service';

@Component({
  selector: 'app-view-credit-card-page',
  imports: [
    ProgressSpinner,
    FinancesTitleBarComponent,
    TranslatePipe,
    AdvancedDatePickerComponent,
    ReactiveFormsModule,
    WalletEntryTableComponent,
    LocalDatePipe,
    DashboardCardComponent,
    LocalCurrencyPipe,
  ],
  templateUrl: './view-credit-card-page.component.html',
  styleUrl: './view-credit-card-page.component.scss',
})
@UntilDestroy()
export class ViewCreditCardPageComponent {
  public readonly balanceIcon = faDollarSign;
  public readonly closingIcon = faCalendarMinus;
  public readonly dueIcon = faCalendarXmark;
  readonly dateControl = new FormControl<DateRange | undefined>(undefined);

  titleBarButtons: FinancesTitleBarExtraButton[] = [];
  creditCard: CreditCardDto | undefined = undefined;
  creditCardBill: CreditCardBillDto | undefined = undefined;

  dateRange: DateRange | undefined = undefined;

  private currentFilter: { creditCard: string; billDate: dayjs.Dayjs } | undefined = undefined;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private creditCardService: CreditCardService,
    private messageService: MessageService,
    private translateService: TranslateService,
    private errorMessageService: ErrorMessageService,
    private walletEntryService: WalletEntryService,
    private creditCardBillService: CreditCardBillService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getCreditCard(id);
      } else {
        this.goToNotFound();
      }
    });

    this.dateControl.valueChanges.pipe(untilDestroyed(this), startWith(this.dateControl.value)).subscribe(date => {
      this.dateRange = date == null ? undefined : date;
      this.getCreditCardBill();
    });
  }

  private async getCreditCardBill(): Promise<CreditCardBillDto | undefined> {
    console.log('----');
    console.log(this.dateRange);
    console.log(this.creditCard);
    console.log('----');
    if (
      this.dateRange == null ||
      this.creditCard == null ||
      (this.currentFilter?.creditCard === this.creditCard.id && this.currentFilter?.billDate.isSame(this.dateRange.startDate))
    ) {
      return undefined;
    }

    this.creditCardBill = undefined;

    this.currentFilter = {
      creditCard: this.creditCard.id,
      billDate: this.dateRange.startDate,
    };

    this.creditCardBill = await this.creditCardBillService.getBillForMonth(
      this.creditCard.id,
      this.dateRange.startDate.month() + 1,
      this.dateRange.startDate.year(),
    );

    return this.creditCardBill;
  }

  private async getCreditCard(id: string): Promise<void> {
    try {
      this.creditCard = await this.creditCardService.getCreditCard(id);

      const billDate = this.creditCardBillService.getBestBill(
        dayjs(),
        this.creditCard.dueDay,
        this.creditCard.dueOnNextBusinessDay,
        this.creditCard.daysBetweenDueAndClosing,
      );

      this.dateControl.setValue({
        startDate: billDate,
        endDate: billDate,
        sameMonth: true,
      });

      this.getCreditCardBill();

      this.titleBarButtons = [
        {
          routerLink: `../edit/${id}`,
          rounded: true,
          tooltip: 'general.edit',
          icon: faPencil,
        },
      ];

      // this.getSummary();
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
