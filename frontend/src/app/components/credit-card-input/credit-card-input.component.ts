import { Component, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlContainer, ControlValueAccessor, FormControl, NgControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
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

@Component({
  selector: 'app-credit-card-input',
  templateUrl: './credit-card-input.component.html',
  styleUrls: ['./credit-card-input.component.scss'],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => CreditCardInputComponent), multi: true }],
})
export class CreditCardInputComponent extends ControlValueAccessorConnector<CreditCardInputValue> implements OnInit, ControlValueAccessor {
  creditCardsWithPersons$: Observable<CreditCardWithPerson[]>;

  private creditCardFromOtherUsers = new BehaviorSubject<CreditCardWithPerson[]>([]);
  private _creditCards: CreditCard[];

  @Input() group: Group;
  @Input() formControl: FormControl;
  @Input() formControlName: string;

  @Output() creditCardsChange = new BehaviorSubject<CreditCard[]>([]);

  get creditCards() {
    return this._creditCards;
  }

  constructor(private creditCardSelectors: CreditCardSelectors, private authSelectors: AuthSelectors, controlContainer: ControlContainer) {
    super(controlContainer);
  }

  ngOnInit(): void {
    this.mountCreditCardFromOtherUsers();
    this.mountCreditCards();
  }

  creditCardInputValueCompare(obj1: any, obj2: any) {
    return obj1 === obj2 || (obj1 && obj2 && obj1.creditCardId === obj2.creditCardId && obj1.creditsPerson === obj2.creditsPerson);
  }

  selectCreditCard(creditCardId: string) {
    this.creditCardsWithPersons$.pipe(take(1)).subscribe(creditCardsWithPersons => {
      const creditCardWithPerson = creditCardsWithPersons.find(
        creditCardsWithPerson => creditCardsWithPerson.creditCards.find(creditCard => creditCard.id === creditCardId) != null,
      );

      this.control.setValue({ creditCardId: creditCardId, personId: creditCardWithPerson.person.id });
    });
  }

  private mountCreditCards(): void {
    this.creditCardsWithPersons$ = combineLatest([
      this.creditCardSelectors.creditCards$,
      this.authSelectors.user$,
      this.creditCardFromOtherUsers.asObservable(),
    ]).pipe(
      map(combined => ({ creditCards: combined[0], user: combined[1], creditCardFromOtherUsers: combined[2] })),
      map(combined => {
        this._creditCards = combined.creditCards;
        this.creditCardsChange.next(this.creditCards);

        return [
          {
            person: combined.user,
            creditCards: combined.creditCards.sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name)),
          },
          ...combined.creditCardFromOtherUsers,
        ].sort((a, b) => a.person.name.localeCompare(b.person.name));
      }),
    );
  }

  private async mountCreditCardFromOtherUsers() {
    const user = await this.authSelectors.currentUser();

    const creditCardFromOtherUsers: CreditCardWithPerson[] = [];

    if (this.group != null) {
      this.group.users.forEach(userFromGroup => {
        if (userFromGroup.id !== user.id) {
          if (userFromGroup.creditCards?.length > 0) {
            creditCardFromOtherUsers.push({ person: userFromGroup, creditCards: userFromGroup.creditCards });
          }
        }
      });
    }
  }
}
