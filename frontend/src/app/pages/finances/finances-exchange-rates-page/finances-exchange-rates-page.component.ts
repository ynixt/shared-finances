import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import dayjs from 'dayjs';
import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinner } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';

import { CurrencySelectorComponent } from '../../../components/currency-selector/currency-selector.component';
import { InfinitePaginatorComponent } from '../../../components/infinite-paginator/infinite-paginator.component';
import { CursorPage } from '../../../models/cursor-pagination';
import { ExchangeRateQuoteDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exchangerate/exchange-rate-quote-dto';
import { LocalCurrencyPipe } from '../../../pipes/local-currency.pipe';
import { LocalDatePipe } from '../../../pipes/local-date.pipe';
import { ErrorMessageService } from '../../../services/error-message.service';
import { ONLY_DATE_FORMAT } from '../../../util/date-util';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { ExchangeRateService } from '../services/exchange-rate.service';

@Component({
  selector: 'app-finances-exchange-rates-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ProgressSpinner,
    TableModule,
    LocalDatePipe,
    FormsModule,
    InputTextModule,
    ButtonDirective,
    InfinitePaginatorComponent,
    CurrencySelectorComponent,
    DatePicker,
    LocalCurrencyPipe,
  ],
  templateUrl: './finances-exchange-rates-page.component.html',
  styleUrl: './finances-exchange-rates-page.component.scss',
})
export class FinancesExchangeRatesPageComponent {
  loading = false;
  quotes: ExchangeRateQuoteDto[] = [];
  showPaginator = true;

  readonly pageSize = 20;
  readonly pages: Map<number, CursorPage<ExchangeRateQuoteDto>> = new Map();
  currentPageNumber = 0;
  page: CursorPage<ExchangeRateQuoteDto> | undefined;

  filterBase = '';
  filterQuote: string = '';
  filterDateFrom: Date | undefined = undefined;

  constructor(
    private exchangeRateService: ExchangeRateService,
    private errorMessageService: ErrorMessageService,
    private messageService: MessageService,
  ) {
    void this.loadInitialData();
  }

  private filterPayload() {
    const date = this.filterDateFrom != null ? dayjs(this.filterDateFrom).format(ONLY_DATE_FORMAT) : undefined;

    return {
      baseCurrency: this.filterBase.trim() || undefined,
      quoteCurrency: this.filterQuote.trim() || undefined,
      quoteDateFrom: date || undefined,
      quoteDateTo: date || undefined,
    };
  }

  async applyFilters() {
    this.showPaginator = false;
    await this.loadInitialData();
    this.showPaginator = true;
  }

  async onPageChange(newPage: number) {
    if (this.pages.has(newPage)) {
      this.page = this.pages.get(newPage);
      this.currentPageNumber = newPage;
      this.quotes = this.page?.items ?? [];
    } else {
      const nextCursor = this.page?.nextCursor;
      this.page = undefined;
      this.loading = true;
      try {
        const loaded = await this.exchangeRateService.list({ size: this.pageSize, nextCursor: nextCursor }, this.filterPayload());
        this.page = loaded;
        this.pages.set(++this.currentPageNumber, loaded);
        this.quotes = loaded.items;
      } catch (error) {
        this.errorMessageService.handleError(error, this.messageService);
      } finally {
        this.loading = false;
      }
    }
  }

  private async loadInitialData() {
    this.currentPageNumber = 0;
    this.page = undefined;
    this.pages.clear();
    this.loading = true;

    try {
      const first = await this.exchangeRateService.list({ size: this.pageSize }, this.filterPayload());
      this.page = first;
      this.pages.set(0, first);
      this.quotes = first.items;
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.quotes = [];
    } finally {
      this.loading = false;
    }
  }
}
