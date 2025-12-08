import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faCalendarMinus, faCalendarXmark, faDollarSign, faPencil } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { startWith } from 'rxjs';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { ProgressSpinner } from 'primeng/progressspinner';

import {
  CreditCardBillDto,
  CreditCardDto,
} from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { LocalCurrencyPipe } from '../../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { ONLY_DATE_FORMAT } from '../../../../util/date-util';
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
    Button,
    FaIconComponent,
    Dialog,
  ],
  templateUrl: './view-credit-card-page.component.html',
  styleUrl: './view-credit-card-page.component.scss',
})
@UntilDestroy()
export class ViewCreditCardPageComponent {
  public readonly balanceIcon = faDollarSign;
  public readonly closingIcon = faCalendarMinus;
  public readonly dueIcon = faCalendarXmark;
  public readonly editIcon = faPencil;

  readonly dateControl = new FormControl<DateRange | undefined>(undefined);
  readonly formToEditClosingDay = new FormGroup({ closingDay: new FormControl<DateRange | undefined>(undefined, [Validators.required]) });
  readonly formToEditDueDay = new FormGroup({ dueDay: new FormControl<DateRange | undefined>(undefined, [Validators.required]) });

  titleBarButtons: FinancesTitleBarExtraButton[] = [];
  creditCard: CreditCardDto | undefined = undefined;
  creditCardBill: CreditCardBillDto | undefined = undefined;

  dateRange: DateRange | undefined = undefined;
  dialogToEditClosingDayIsVisible = false;
  dialogToEditClosingDayIsSubmitting = false;
  dialogToEditDueDayIsVisible = false;
  dialogToEditDueDayIsSubmitting = false;

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

  openEditClosingDateDialog() {
    if (this.creditCardBill) {
      this.formToEditClosingDay.reset();
      this.formToEditClosingDay.get('closingDay')!!.setValue({ startDate: dayjs(this.creditCardBill.closingDate) });
      this.dialogToEditClosingDayIsVisible = true;
    }
  }

  async submitNewClosingDate() {
    if (!this.formToEditClosingDay.valid || !this.creditCardBill || this.creditCardBill.id == null) return;

    const closingDate = this.formToEditClosingDay.get('closingDay')?.value?.startDate;

    if (closingDate) {
      try {
        this.dialogToEditClosingDayIsSubmitting = true;
        await this.creditCardBillService.changeClosingDate(this.creditCardBill.id, closingDate);

        this.creditCardBill = {
          ...this.creditCardBill,
          closingDate: closingDate.format(ONLY_DATE_FORMAT),
        };
      } catch (error) {
        this.errorMessageService.handleError(error, this.messageService);

        throw error;
      } finally {
        this.dialogToEditClosingDayIsSubmitting = false;
      }
    }

    this.dialogToEditClosingDayIsVisible = false;
  }

  openEditDueDateDialog() {
    if (this.creditCardBill) {
      this.formToEditDueDay.reset();
      this.formToEditDueDay.get('dueDay')!!.setValue({ startDate: dayjs(this.creditCardBill.dueDate) });
      this.dialogToEditDueDayIsVisible = true;
    }
  }

  async submitNewDueDate() {
    if (!this.formToEditDueDay.valid || !this.creditCardBill || this.creditCardBill.id == null) return;

    const dueDate = this.formToEditDueDay.get('dueDay')?.value?.startDate;

    if (dueDate) {
      try {
        this.dialogToEditDueDayIsSubmitting = true;
        await this.creditCardBillService.changeDueDate(this.creditCardBill.id, dueDate);

        this.creditCardBill = {
          ...this.creditCardBill,
          dueDate: dueDate.format(ONLY_DATE_FORMAT),
        };
      } catch (error) {
        this.errorMessageService.handleError(error, this.messageService);

        throw error;
      } finally {
        this.dialogToEditDueDayIsSubmitting = false;
      }
    }

    this.dialogToEditDueDayIsVisible = false;
  }

  private async getCreditCardBill(): Promise<CreditCardBillDto | undefined> {
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
