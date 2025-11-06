import { Injectable } from '@angular/core';

import { Observable, filter, map } from 'rxjs';

import { GroupWithRoleDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { ActionEventType } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { UserActionEventService } from './user-action-event.service';

export interface GroupActionEvent<T> {
  data: T;
  groupId: string;
  id: string;
  modifiedByUserId: string;
  type: ActionEventType;
}

@Injectable({ providedIn: 'root' })
export class GroupsActionEventService {
  readonly groupUpdated$: Observable<GroupActionEvent<GroupWithRoleDto>>;
  readonly groupDeleted$: Observable<GroupActionEvent<string>>;

  readonly bankAccountAssociated$: Observable<GroupActionEvent<string>>;
  readonly bankAccountUnassociated$: Observable<GroupActionEvent<string>>;

  readonly creditCardAssociated$: Observable<GroupActionEvent<string>>;
  readonly creditCardUnassociated$: Observable<GroupActionEvent<string>>;

  constructor(private userActionEventService: UserActionEventService) {
    const baseGroup$ = this.userActionEventService.groupEvents$.pipe(filter(g => g.event === 'GROUP'));

    this.groupUpdated$ = baseGroup$.pipe(
      filter(e => e.type === 'UPDATE'),
      map(e => e as GroupActionEvent<GroupWithRoleDto>),
    );

    this.groupDeleted$ = baseGroup$.pipe(
      filter(e => e.type === 'DELETE'),
      map(e => e as GroupActionEvent<string>),
    );

    const baseBankAccountAssociate$ = this.userActionEventService.groupEvents$.pipe(filter(g => g.event === 'BANK_ACCOUNT_ASSOCIATE'));

    this.bankAccountAssociated$ = baseBankAccountAssociate$.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e as GroupActionEvent<string>),
    );

    this.bankAccountUnassociated$ = baseBankAccountAssociate$.pipe(
      filter(e => e.type === 'DELETE'),
      map(e => e as GroupActionEvent<string>),
    );

    const baseCreditCardAssociate$ = this.userActionEventService.groupEvents$.pipe(filter(g => g.event === 'CREDIT_CARD_ASSOCIATE'));

    this.creditCardAssociated$ = baseCreditCardAssociate$.pipe(
      filter(e => e.type === 'INSERT'),
      map(e => e as GroupActionEvent<string>),
    );

    this.creditCardUnassociated$ = baseCreditCardAssociate$.pipe(
      filter(e => e.type === 'DELETE'),
      map(e => e as GroupActionEvent<string>),
    );
  }
}
