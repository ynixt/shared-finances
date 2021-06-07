import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { CreditCard, User } from 'src/app/@core/models';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-credit-card',
  templateUrl: './credit-card.component.html',
  styleUrls: ['./credit-card.component.scss'],
})
export class CreditCardComponent implements OnInit, OnDestroy {
  creditCards: CreditCard[] = [];

  private userSubscription: Subscription;

  constructor(private authSelector: AuthSelectors) {}

  async ngOnInit(): Promise<void> {
    this.userSubscription = this.authSelector.user$.subscribe(user => this.fillCreditCards(user));
  }

  ngOnDestroy(): void {
    if (this.userSubscription) {
      this.userSubscription.unsubscribe();
    }
  }

  private fillCreditCards(user: User): void {
    this.creditCards = user.creditCards || [];
  }
}
