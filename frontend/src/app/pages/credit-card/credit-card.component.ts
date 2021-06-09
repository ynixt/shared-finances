import { Component, OnDestroy, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { from, of, Subscription } from 'rxjs';
import { take } from 'rxjs/operators';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { CreditCard, User } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { AuthDispatchers } from 'src/app/store';
import { AuthSelectors } from 'src/app/store/services/selectors';
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
    private authSelector: AuthSelectors,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private authDispatchers: AuthDispatchers,
    private authSelectors: AuthSelectors,
  ) {}

  async ngOnInit(): Promise<void> {
    this.authSelector.user$.pipe(untilDestroyed(this)).subscribe(user => this.fillCreditCards(user));
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
        await this.applyCreditCardRemovedOnUnser(creditCard);
      }
    }
  }

  private async applyCreditCardRemovedOnUnser(creditCardRemoved: CreditCard): Promise<void> {
    const currentUser: User = { creditCards: [], ...(await this.authSelectors.currentUser()) };

    currentUser.creditCards = currentUser.creditCards.filter(creditCard => creditCard.id !== creditCardRemoved.id);

    this.authDispatchers.userUpdated(currentUser);
  }

  private fillCreditCards(user: User): void {
    this.creditCards = [...(user.creditCards || [])].sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name));
  }
}
