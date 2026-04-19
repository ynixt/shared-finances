import { HttpErrorResponse } from '@angular/common/http';
import '@angular/compiler';

import { of, throwError } from 'rxjs';

import { describe, expect, it, vi } from 'vitest';

import { UserResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { BankAccountForGroupAssociateDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/bankAccount';
import { CreditCardForGroupAssociateDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { UserMissingError } from '../errors/user-missing.error';
import { GroupAssociationService } from './group-association.service';

function buildUser(): UserResponseDto {
  return {
    id: 'user-1',
    firstName: 'Owner',
    lastName: 'User',
    email: 'owner@example.com',
    defaultCurrency: 'BRL',
    emailVerified: true,
    lang: 'pt-BR',
    mfaEnabled: false,
    onboardingDone: true,
    tmz: 'America/Sao_Paulo',
    photoUrl: null,
  };
}

function buildAssociatedBank(id: string): BankAccountForGroupAssociateDto {
  return {
    id,
    name: `Bank ${id}`,
    currency: 'BRL',
    user: {
      id: 'user-1',
      firstName: 'Owner',
      lastName: 'User',
      email: 'owner@example.com',
      photoUrl: null,
    },
  };
}

function buildAssociatedCreditCard(id: string): CreditCardForGroupAssociateDto {
  return {
    id,
    name: `Card ${id}`,
    currency: 'BRL',
    user: {
      id: 'user-1',
      firstName: 'Owner',
      lastName: 'User',
      email: 'owner@example.com',
      photoUrl: null,
    },
  };
}

describe('GroupAssociationService', () => {
  it('loads allowed bank accounts from owner-scoped endpoint', async () => {
    const expected = [buildAssociatedBank('bank-owner-1')];
    const http = {
      get: vi.fn().mockReturnValue(of(expected)),
      put: vi.fn(),
      delete: vi.fn(),
    };
    const userService = {
      getUser: vi.fn().mockResolvedValue(buildUser()),
    };
    const service = new GroupAssociationService(http as any, userService as any);

    const result = await service.findAllAllowedBanksToAssociate('group-1');

    expect(result).toEqual(expected);
    expect(http.get).toHaveBeenCalledWith('/api/groups/group-1/associations/banks/allowed');
  });

  it('loads allowed credit cards from owner-scoped endpoint', async () => {
    const expected = [buildAssociatedCreditCard('card-owner-1')];
    const http = {
      get: vi.fn().mockReturnValue(of(expected)),
      put: vi.fn(),
      delete: vi.fn(),
    };
    const userService = {
      getUser: vi.fn().mockResolvedValue(buildUser()),
    };
    const service = new GroupAssociationService(http as any, userService as any);

    const result = await service.findAllAllowedCreditCardsToAssociate('group-2');

    expect(result).toEqual(expected);
    expect(http.get).toHaveBeenCalledWith('/api/groups/group-2/associations/creditCards/allowed');
  });

  it('propagates backend unauthorized error when associating bank by stale or forged id', async () => {
    const backendError = new HttpErrorResponse({
      status: 403,
      statusText: 'Forbidden',
    });
    const http = {
      get: vi.fn(),
      put: vi.fn().mockReturnValue(throwError(() => backendError)),
      delete: vi.fn(),
    };
    const userService = {
      getUser: vi.fn().mockResolvedValue(buildUser()),
    };
    const service = new GroupAssociationService(http as any, userService as any);

    await expect(service.associateBank('group-1', 'bank-foreign')).rejects.toBe(backendError);
    expect(http.put).toHaveBeenCalledWith('/api/groups/group-1/associations/banks/bank-foreign', undefined);
  });

  it('throws UserMissingError when no authenticated user exists', async () => {
    const http = {
      get: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    };
    const userService = {
      getUser: vi.fn().mockResolvedValue(null),
    };
    const service = new GroupAssociationService(http as any, userService as any);

    await expect(service.findAllAllowedBanksToAssociate('group-1')).rejects.toBeInstanceOf(UserMissingError);
    expect(http.get).not.toHaveBeenCalled();
  });
});
