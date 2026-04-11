import '@angular/compiler';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';

import { NEVER } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ViewCreditCardPageComponent } from './view-credit-card-page.component';

describe('ViewCreditCardPageComponent', () => {
  const routerMock = {
    navigateByUrl: vi.fn().mockResolvedValue(true),
  };

  const creditCardServiceMock = {
    getCreditCard: vi.fn(),
  };

  const messageServiceMock = {
    add: vi.fn(),
  };

  const translateServiceMock = {
    instant: vi.fn((key: string) => key),
  };

  const errorMessageServiceMock = {
    handleError: vi.fn(),
  };

  const walletEntryServiceMock = {};

  const creditCardBillServiceMock = {
    payBill: vi.fn().mockResolvedValue(undefined),
  };

  const userActionEventServiceMock = {
    transactionInserted$: NEVER,
    transactionUpdated$: NEVER,
    transactionDeleted$: NEVER,
  };

  const walletItemServiceMock = {
    getAllItems: vi.fn(),
  };

  const activatedRouteMock = {
    paramMap: NEVER,
    snapshot: {
      queryParamMap: convertToParamMap({}),
    },
  };

  function createComponent() {
    return new ViewCreditCardPageComponent(
      activatedRouteMock as unknown as ActivatedRoute,
      routerMock as unknown as Router,
      creditCardServiceMock as never,
      messageServiceMock as never,
      translateServiceMock as never,
      errorMessageServiceMock as never,
      walletEntryServiceMock as never,
      creditCardBillServiceMock as never,
      userActionEventServiceMock as never,
      walletItemServiceMock as never,
    );
  }

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('pre-fills the pay bill dialog with the remaining amount and a max validator', () => {
    const component = createComponent();

    component.creditCardBill.set({
      id: 'bill-1',
      value: -120,
    } as never);

    component.openPayBillDialog();

    expect(component.dialogToPayBillIsVisible).toBe(true);
    expect(component.formToPayBill.controls.amount.value).toBe(120);
    expect(component.formToPayBill.controls.date.value).toBeInstanceOf(Date);

    component.formToPayBill.controls.amount.setValue(121);

    expect(component.formToPayBill.controls.amount.errors?.['max']).toBeTruthy();
  });

  it('submits an explicit bill payment and refreshes the bill state', async () => {
    const component = createComponent();
    const refreshBillMock = vi.fn().mockResolvedValue(undefined);
    const refreshCreditCardMock = vi.fn().mockResolvedValue(undefined);

    (component as unknown as { getCreditCardBill: typeof refreshBillMock }).getCreditCardBill = refreshBillMock;
    (component as unknown as { refreshCreditCard: typeof refreshCreditCardMock }).refreshCreditCard = refreshCreditCardMock;

    component.creditCardBill.set({
      id: 'bill-1',
      value: -120,
    } as never);
    component.dialogToPayBillIsVisible = true;
    component.formToPayBill.controls.bankAccount.setValue({
      id: 'bank-1',
      name: 'Main account',
      type: 'BANK_ACCOUNT',
    } as never);
    component.formToPayBill.controls.date.setValue(new Date(2026, 3, 8));
    component.formToPayBill.controls.amount.setValue(50);
    component.formToPayBill.controls.observations.setValue('  partial payment  ');

    await component.submitPayBill();

    expect(creditCardBillServiceMock.payBill).toHaveBeenCalledWith('bill-1', {
      amount: 50,
      bankAccountId: 'bank-1',
      date: '2026-04-08',
      observations: 'partial payment',
    });
    expect(messageServiceMock.add).toHaveBeenCalledWith({
      severity: 'success',
      summary: 'financesPage.creditCardsPage.viewCreditCardPage.payBillDialog.successMessage',
    });
    expect(component.dialogToPayBillIsVisible).toBe(false);
    expect(component.dialogToPayBillIsSubmitting).toBe(false);
    expect(refreshBillMock).toHaveBeenCalledWith(true);
    expect(refreshCreditCardMock).toHaveBeenCalledTimes(1);
  });

  it('loads only bank accounts for the pay bill account picker', async () => {
    const component = createComponent();

    walletItemServiceMock.getAllItems.mockResolvedValue({
      content: [
        { id: 'bank-1', name: 'Main account', type: 'BANK_ACCOUNT' },
        { id: 'card-1', name: 'Travel card', type: 'CREDIT_CARD' },
      ],
    });

    const result = await component.loadBankAccounts(2);

    expect(walletItemServiceMock.getAllItems).toHaveBeenCalledWith({ sort: 'name', page: 2 });
    expect(result).toHaveLength(1);
    expect(result[0]).toMatchObject({
      id: 'bank-1',
      name: 'Main account',
      type: 'BANK_ACCOUNT',
    });
    expect(result[0]?.icon).toBe(component.bankAccountIcon);
  });
});
