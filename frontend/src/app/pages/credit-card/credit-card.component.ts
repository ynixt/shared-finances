import { Component, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { CreditCard } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { CreditCardSelectors } from 'src/app/store/services/selectors';
import { CreditCardService } from './credit-card.service';
import { CreditCardState } from 'src/app/store/reducers/credit-card.reducer';

@UntilDestroy()
@Component({
  selector: 'app-credit-card',
  templateUrl: './credit-card.component.html',
  styleUrls: ['./credit-card.component.scss'],
})
export class CreditCardComponent implements OnInit {
  creditCards: CreditCard[] = [];
  creditCardsLoading = true;

  constructor(
    private creditCardService: CreditCardService,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private creditCardSelectors: CreditCardSelectors,
  ) {}

  async ngOnInit(): Promise<void> {
    this.creditCardSelectors.state$.pipe(untilDestroyed(this)).subscribe(creditCardsState => this.fillCreditCards(creditCardsState));
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
      await this.creditCardService
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
    }
  }

  private fillCreditCards(creditCardState: CreditCardState): void {
    this.creditCardsLoading = creditCardState.loading;
    this.creditCards = [...(creditCardState.creditCards || [])].sort((creditCardA, creditCardB) =>
      creditCardA.name.localeCompare(creditCardB.name),
    );
  }
}
