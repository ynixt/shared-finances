import { Injectable, NgZone, OnDestroy } from '@angular/core';

import { Observable, filter, map } from 'rxjs';

import { UserActionEventDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/events';
import { GroupDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { BankAccountDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { CreditCardDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { EventForListDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ActionEventCategory } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { TokenStateService } from '../../../services/token-state.service';
import { ActionEventService } from './action-event.service';

const notGroupFilter = filter((e: UserActionEventDto) => e.groupId == null);

export type GroupActionEventDto = UserActionEventDto & {
  event: ActionEventCategory;
  groupId?: string;
  modifiedByUserId?: string;
};

@Injectable({ providedIn: 'root' })
export class UserActionEventService extends ActionEventService implements OnDestroy {
  readonly groupEvents$: Observable<GroupActionEventDto>;

  readonly bankInserted$: Observable<BankAccountDto>;
  readonly bankUpdated$: Observable<BankAccountDto>;
  readonly bankDeleted$: Observable<string>;

  readonly creditCardInserted$: Observable<CreditCardDto>;
  readonly creditCardUpdated$: Observable<CreditCardDto>;
  readonly creditCardDeleted$: Observable<string>;

  readonly groupInserted$: Observable<GroupDto>;

  readonly transactionInserted$: Observable<EventForListDto>;

  constructor(tokenStateService: TokenStateService, zone: NgZone) {
    super(tokenStateService, zone, '/api/sse/user-events');

    this.groupEvents$ = this.wire$.pipe(
      map(msg => {
        return { ...JSON.parse(msg.data as any as string), event: msg.event } as GroupActionEventDto;
      }),
      filter((e: GroupActionEventDto) => e.groupId != null),
    );

    // --- Bank ---
    const baseBank$ = this.streamOf<UserActionEventDto>('BANK_ACCOUNT').pipe(notGroupFilter);

    this.bankInserted$ = baseBank$.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e.data as BankAccountDto),
    );

    this.bankUpdated$ = baseBank$.pipe(
      filter(e => e.type === 'UPDATE'),
      map(e => e.data as BankAccountDto),
    );

    this.bankDeleted$ = baseBank$.pipe(
      filter(e => e.type === 'DELETE'),
      map(e => e.data as string),
    );

    // --- CreditCard ---
    const baseCreditCard$ = this.streamOf<UserActionEventDto>('CREDIT_CARD').pipe(notGroupFilter);

    this.creditCardInserted$ = baseCreditCard$.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e.data as CreditCardDto),
    );

    this.creditCardUpdated$ = baseCreditCard$.pipe(
      filter(e => e.type === 'UPDATE'),
      map(e => e.data as CreditCardDto),
    );

    this.creditCardDeleted$ = baseCreditCard$.pipe(
      filter(e => e.type === 'DELETE'),
      map(e => e.data as string),
    );

    // --- Group ---
    const baseGroup$ = this.streamOf<UserActionEventDto>('GROUP').pipe(notGroupFilter);

    this.groupInserted$ = baseGroup$.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e.data as GroupDto),
    );

    // --- Transaction ---
    const baseTransaction = this.streamOf<UserActionEventDto>('WALLET_EVENT').pipe(notGroupFilter);

    this.transactionInserted$ = baseTransaction.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e.data as EventForListDto),
    );
  }
}
