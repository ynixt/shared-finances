import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CreditCard, User } from 'src/app/@core/models';
import { AuthDispatchers } from 'src/app/store';
import { AuthSelectors } from 'src/app/store/services/selectors';
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
    private authSelectors: AuthSelectors,
    private authDispatchers: AuthDispatchers,
  ) {}

  ngOnInit(): void {}

  async newCreditCard(creditCardInput: CreditCard): Promise<void> {
    const creditCardSaved = await this.creditCardService.newCreditCard(creditCardInput);
    await this.applyNewCreditCardToUser(creditCardSaved);
    this.router.navigateByUrl('/credit');
  }

  private async applyNewCreditCardToUser(newCreditCard: CreditCard): Promise<void> {
    const currentUser: User = { creditCards: [], ...(await this.authSelectors.currentUser()) };

    currentUser.creditCards = [...currentUser.creditCards, newCreditCard];

    this.authDispatchers.userUpdated(currentUser);
  }
}
