import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { CreditCard } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { CreditCardDispatchers } from 'src/app/store';
import { CreditCardService } from '../credit-card.service';

@Component({
  selector: 'app-new-credit-card',
  templateUrl: './new-credit-card.component.html',
  styleUrls: ['./new-credit-card.component.scss'],
})
export class NewCreditCardComponent implements OnInit {
  constructor(
    private creditCardService: CreditCardService,
    private router: Router,
    private creditCardDispatchers: CreditCardDispatchers,
    private toast: HotToastService,
    private translocoService: TranslocoService,
    private errorService: ErrorService,
  ) {}

  ngOnInit(): void {}

  async newCreditCard(creditCardInput: CreditCard): Promise<void> {
    const creditCardSaved = await this.creditCardService
      .newCreditCard(creditCardInput)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('creating'),
          success: this.translocoService.translate('creating-successful', { name: creditCardInput.name }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'creating-error', 'creating-error-with-description', {
              name: creditCardInput.name,
            }),
        }),
      )
      .toPromise();

    this.creditCardDispatchers.creditCardAdded(creditCardSaved);
    this.router.navigateByUrl('/credit');
  }
}
