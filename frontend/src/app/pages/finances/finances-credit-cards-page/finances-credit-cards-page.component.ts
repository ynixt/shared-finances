import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faPenToSquare } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { throttleTime } from 'rxjs';

import { ButtonDirective } from 'primeng/button';
import { Card } from 'primeng/card';
import { DataView, DataViewLazyLoadEvent } from 'primeng/dataview';
import { Tooltip } from 'primeng/tooltip';

import { BankAccountDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { CreditCardDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { Page } from '../../../models/pagination';
import { LocalCurrencyPipe } from '../../../pipes/local-currency.pipe';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { CreditCardService } from '../services/credit-card.service';
import { UserActionEventService } from '../services/user-action-event.service';

@Component({
  selector: 'app-finances-credit-cards-page',
  imports: [
    TranslatePipe,
    FinancesTitleBarComponent,
    DataView,
    Card,
    LocalCurrencyPipe,
    ButtonDirective,
    FaIconComponent,
    RouterLink,
    Tooltip,
  ],
  templateUrl: './finances-credit-cards-page.component.html',
  styleUrl: './finances-credit-cards-page.component.scss',
})
@UntilDestroy()
export class FinancesCreditCardsPageComponent implements OnInit {
  readonly editFinanceIcon = faPenToSquare;
  readonly openFinanceIcon = faArrowUpRightFromSquare;
  readonly pageSize = 12;

  loading = true;
  items: Page<CreditCardDto> = createEmptyPage();

  private currentPage = 0;

  constructor(
    private creditCardService: CreditCardService,
    userActionEventService: UserActionEventService,
  ) {
    userActionEventService.creditCardInserted$.pipe(untilDestroyed(this), throttleTime(100)).subscribe(this.creditCardInserted.bind(this));
    userActionEventService.creditCardUpdated$.pipe(untilDestroyed(this)).subscribe(this.creditCardUpdated.bind(this));
    userActionEventService.creditCardDeleted$.pipe(untilDestroyed(this)).subscribe(this.creditCardDeleted.bind(this));
  }

  ngOnInit() {
    this.loadPage(0);
  }

  private async loadPage(page = 0) {
    this.loading = true;
    this.currentPage = page;

    this.items = await this.creditCardService.getAllCreditCards({
      page,
      size: this.pageSize,
      sort: [
        {
          property: 'name',
          direction: 'ASC',
        },
      ],
    });

    this.loading = false;
  }

  onLazyLoad(event: DataViewLazyLoadEvent) {
    const newPage = this.items.totalPages == 0 ? 0 : Math.floor(event.first / this.items.numberOfElements);
    this.loadPage(newPage);
  }

  private creditCardInserted(_: CreditCardDto) {
    if (this.currentPage === 0) {
      this.loadPage();
    }
  }

  private creditCardUpdated(creditCardAccountDto: CreditCardDto) {
    const index = this.items.content.findIndex(b => b.id === creditCardAccountDto.id);

    if (index != -1) {
      const newItems = [...this.items.content];
      newItems[index] = creditCardAccountDto;

      this.items = {
        ...this.items,
        content: newItems,
      };
    }
  }

  private creditCardDeleted(id: string) {
    const index = this.items.content.findIndex(b => b.id === id);

    if (index != -1) {
      const newItems = [...this.items.content];
      newItems.splice(index, 1);

      this.items = {
        ...this.items,
        content: newItems,
      };
    }
  }
}
