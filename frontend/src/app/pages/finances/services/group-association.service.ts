import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { OnlyIdDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto';
import { GroupInviteDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { GroupInfoForInviteDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups/invite';
import { BankAccountForGroupAssociateDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { CreditCardForGroupAssociateDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class GroupAssociationService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
  ) {}
  async findAllAssociatedBanks(groupId: string): Promise<BankAccountForGroupAssociateDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.get<BankAccountForGroupAssociateDto[]>(`/api/groups/${groupId}/associations/banks`).pipe(take(1)));
    }

    throw new UserMissingError();
  }

  async findAllAllowedBanksToAssociate(groupId: string): Promise<BankAccountForGroupAssociateDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.http.get<BankAccountForGroupAssociateDto[]>(`/api/groups/${groupId}/associations/banks/allowed`).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }

  async associateBank(groupId: string, bankAccountId: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.put<void>(`/api/groups/${groupId}/associations/banks/${bankAccountId}`, undefined).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async unassociateBank(groupId: string, bankAccountId: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.delete<void>(`/api/groups/${groupId}/associations/banks/${bankAccountId}`).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async findAllAssociatedCreditCards(groupId: string): Promise<CreditCardForGroupAssociateDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.http.get<CreditCardForGroupAssociateDto[]>(`/api/groups/${groupId}/associations/creditCards`).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }

  async findAllAllowedCreditCardsToAssociate(groupId: string): Promise<CreditCardForGroupAssociateDto[]> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(
        this.http.get<CreditCardForGroupAssociateDto[]>(`/api/groups/${groupId}/associations/creditCards/allowed`).pipe(take(1)),
      );
    }

    throw new UserMissingError();
  }

  async associateCreditCard(groupId: string, creditCardId: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.put<void>(`/api/groups/${groupId}/associations/creditCards/${creditCardId}`, undefined).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }

  async unassociateCreditCard(groupId: string, creditCardId: string): Promise<void> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.delete<void>(`/api/groups/${groupId}/associations/creditCards/${creditCardId}`).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}
