import { Component, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { CreditCard } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { CreditCardDispatchers } from 'src/app/store';
import { CreditCardSelectors } from 'src/app/store/services/selectors';
import { CreditCardService } from './credit-card.service';

@UntilDestroy()
@Component({
  selector: 'app-credit-card',
  templateUrl: './credit-card.component.html',
  styleUrls: ['./credit-card.component.scss'],
})
export class CreditCardComponent implements OnInit {
  creditCards: CreditCard[] = [];

  constructor(
    private creditCardService: CreditCardService,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private creditCardSelectors: CreditCardSelectors,
    private creditCardDispatchers: CreditCardDispatchers,
  ) {}

  async ngOnInit(): Promise<void> {
    this.creditCardSelectors.creditCards$.pipe(untilDestroyed(this)).subscribe(creditCard => this.fillCreditCards(creditCard));
  }

  async delete(creditCard: CreditCard): Promise<void> {
    const confirm = await this.dialogService
      .openConfirm({
        title: this.translocoService.translate('confirm'),
        message: this.translocoService.translate('delete-confirm', { name: creditCard.name }),
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('delete'),
        width: '500px',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();

    if (confirm) {
      this.toast.observe;

      const removed = await this.creditCardService
        .deleteCreditCard(creditCard.id)
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful', { name: creditCard.name }),
            error: error =>
              this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
                name: creditCard.name,
              }),
          }),
        )
        .toPromise();

      if (removed) {
        this.creditCardDispatchers.creditCardRemoved(creditCard.id);
      }
    }
  }

  private fillCreditCards(creditCard: CreditCard[]): void {
    this.creditCards = [...(creditCard || [])].sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name));
  }
}
