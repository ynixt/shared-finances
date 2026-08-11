import { Injectable } from '@angular/core';

import { Observable, filter, map } from 'rxjs';

import { GroupPlanUsageEventDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/events';
import {
  GroupMemberLeftEventDto,
  GroupOwnershipChangedEventDto,
  GroupUpdatedEventDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
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
  readonly groupUpdated$: Observable<GroupActionEvent<GroupUpdatedEventDto>>;
  readonly groupDeleted$: Observable<GroupActionEvent<string>>;
  readonly ownershipChanged$: Observable<GroupActionEvent<GroupOwnershipChangedEventDto>>;
  readonly memberLeft$: Observable<GroupActionEvent<GroupMemberLeftEventDto>>;
  readonly planUsage$: Observable<GroupActionEvent<GroupPlanUsageEventDto>>;

  readonly bankAccountAssociated$: Observable<GroupActionEvent<string>>;
  readonly bankAccountUnassociated$: Observable<GroupActionEvent<string>>;

  readonly creditCardAssociated$: Observable<GroupActionEvent<string>>;
  readonly creditCardUnassociated$: Observable<GroupActionEvent<string>>;

  constructor(private userActionEventService: UserActionEventService) {
    const baseGroup$ = this.userActionEventService.groupEvents$.pipe(filter(g => g.event === 'GROUP'));

    this.groupUpdated$ = baseGroup$.pipe(
      filter(e => e.type === 'UPDATE'),
      map(e => e as GroupActionEvent<GroupUpdatedEventDto>),
    );

    this.ownershipChanged$ = this.userActionEventService.groupEvents$.pipe(
      filter(g => g.event === 'GROUP_OWNERSHIP' && g.type === 'UPDATE'),
      map(e => e as GroupActionEvent<GroupOwnershipChangedEventDto>),
    );

    this.memberLeft$ = this.userActionEventService.groupEvents$.pipe(
      filter(g => g.event === 'GROUP_MEMBERSHIP' && g.type === 'DELETE'),
      map(e => e as GroupActionEvent<GroupMemberLeftEventDto>),
    );

    this.planUsage$ = this.userActionEventService.groupEvents$.pipe(
      filter(g => g.event === 'GROUP_PLAN_USAGE' && g.type === 'UPDATE'),
      map(e => e as GroupActionEvent<GroupPlanUsageEventDto>),
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
