import { Injectable, NgZone, OnDestroy } from '@angular/core';

import { Observable, filter, map } from 'rxjs';

import { UserActionEventDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/events';
import { GroupDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { BankAccountDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { KratosAuthService } from '../../../services/kratos-auth.service';
import { ActionEventService } from './action-event.service';

@Injectable({ providedIn: 'root' })
export class UserActionEventService extends ActionEventService implements OnDestroy {
  readonly bankInserted$: Observable<BankAccountDto>;
  readonly bankUpdated$: Observable<BankAccountDto>;
  readonly bankDeleted$: Observable<string>;

  readonly groupInserted$: Observable<GroupDto>;

  constructor(authService: KratosAuthService, zone: NgZone) {
    super(authService, zone, '/api/sse/user-events');

    // --- Bank ---
    const baseBank$ = this.streamOf<UserActionEventDto>('BANK_ACCOUNT');

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

    // --- Group ---
    const baseGroup$ = this.streamOf<UserActionEventDto>('GROUP');

    this.groupInserted$ = baseGroup$.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e.data as GroupDto),
    );
  }
}
