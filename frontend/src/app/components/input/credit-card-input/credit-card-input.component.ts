import { Component, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { ControlContainer, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { ControlValueAccessorConnector } from 'src/app/@core/control-value-accessor-connector';
import { CreditCard, User } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';
import { AuthSelectors, CreditCardSelectors } from 'src/app/store/services/selectors';

export interface CreditCardInputValue {
  creditCardId: string;
  personId: string;
}

export interface CreditCardWithPerson {
  person: Partial<User>;
  creditCards: CreditCard[];
}

@UntilDestroy()
@Component({
  selector: 'app-credit-card-input',
  templateUrl: './credit-card-input.component.html',
  styleUrls: ['./credit-card-input.component.scss'],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => CreditCardInputComponent), multi: true }],
})
export class CreditCardInputComponent
  extends ControlValueAccessorConnector<CreditCardInputValue>
  implements OnInit, ControlValueAccessor, OnDestroy {
  creditCardsWithPersons$: Observable<CreditCardWithPerson[]>;

  @Input() autoMount = true;

  private creditCardsWithPersonsSubject: BehaviorSubject<CreditCardWithPerson[]>;
  private creditCardFromOtherUsers = new BehaviorSubject<CreditCardWithPerson[]>([]);
  private _creditCards: CreditCard[];
  private creditCardFromOtherUsersSubject: BehaviorSubject<CreditCardWithPerson[]>;

  @Output() creditCardsChange = new BehaviorSubject<CreditCard[]>([]);

  get creditCards() {
    return this._creditCards;
  }

  constructor(private creditCardSelectors: CreditCardSelectors, private authSelectors: AuthSelectors, controlContainer: ControlContainer) {
    super(controlContainer);

    this.creditCardsWithPersonsSubject = new BehaviorSubject([]);
    this.creditCardFromOtherUsersSubject = new BehaviorSubject([]);
    this.creditCardsWithPersons$ = this.creditCardsWithPersonsSubject.asObservable();
  }

  ngOnInit(): void {
    if (this.autoMount) {
      this.mountCreditCards();
    }
  }

  ngOnDestroy(): void {
    this.creditCardsWithPersonsSubject.complete();
  }

  creditCardInputValueCompare(obj1: any, obj2: any): boolean {
    return obj1 === obj2 || (obj1 && obj2 && obj1.creditCardId === obj2.creditCardId && obj1.creditsPerson === obj2.creditsPerson);
  }

  selectCreditCard(creditCardId: string): void {
    this.creditCardsWithPersons$.pipe(take(1)).subscribe(creditCardsWithPersons => {
      const creditCardWithPerson = creditCardsWithPersons.find(
        creditCardsWithPerson => creditCardsWithPerson.creditCards.find(creditCard => creditCard.id === creditCardId) != null,
      );

      if (creditCardWithPerson) {
        this.control.setValue({ creditCardId: creditCardId, personId: creditCardWithPerson.person.id });
      }
    });
  }

  async mountCreditCards(group?: Group): Promise<void> {
    this.mountCreditCardsFromCurrentUser(group != null);
    await this.mountCreditCardFromOtherUsers(group);
  }

  private mountCreditCardsFromCurrentUser(isShared: boolean): void {
    combineLatest([this.creditCardSelectors.creditCards$, this.authSelectors.user$, this.creditCardFromOtherUsersSubject])
      .pipe(
        untilDestroyed(this),
        map(combined => ({ creditCards: combined[0], user: combined[1], creditCardFromOtherUsers: combined[2] })),
        map(combined => {
          this._creditCards = combined.creditCards;
          this.creditCardsChange.next(this.creditCards);

          return [
            {
              person: combined.user,
              creditCards: [...combined.creditCards]
                .filter(creditCard => this.shouldShowCreditCard(creditCard, isShared))
                .sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name)),
            },
            ...combined.creditCardFromOtherUsers,
          ].sort((a, b) => a.person.name.localeCompare(b.person.name));
        }),
      )
      .subscribe(creditCard => {
        this.creditCardsWithPersonsSubject.next(creditCard);
      });
  }

  private async mountCreditCardFromOtherUsers(group?: Group): Promise<void> {
    const user = await this.authSelectors.currentUser();

    if (group != null) {
      const creditCardFromOtherUsers: CreditCardWithPerson[] = [];

      group.users.forEach(userFromGroup => {
        if (userFromGroup.id !== user.id) {
          if (userFromGroup.creditCards?.length > 0) {
            creditCardFromOtherUsers.push({
              person: userFromGroup,
              creditCards: userFromGroup.creditCards.filter(creditCard => this.shouldShowCreditCard(creditCard, true)),
            });
          }
        }
      });

      this.creditCardFromOtherUsersSubject.next(creditCardFromOtherUsers);
    } else {
      this.creditCardFromOtherUsersSubject.next([]);
    }
  }

  private shouldShowCreditCard(creditCard: CreditCard, isShared: boolean): boolean {
    return creditCard.enabled && (!isShared || (isShared && creditCard.displayOnGroup));
  }
}
