import '@angular/compiler';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { PaymentType__Obj, WalletEntryType__Obj } from '../../../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { UserService } from '../../../../../services/user.service';
import { CreditCardBillService } from '../../../services/credit-card-bill.service';
import { GroupService } from '../../../services/group.service';
import { WalletEntryService } from '../../../services/wallet-entry.service';
import { TransactionFormComponent } from './transaction-form.component';

function walletItem(id: string, currency: string) {
  return {
    id,
    name: `Wallet ${id}`,
    type: 'BANK_ACCOUNT',
    enabled: true,
    currency,
    user: null,
  } as any;
}

describe('TransactionFormComponent', () => {
  const fetchTransferRate = vi.fn();

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [TransactionFormComponent, TranslateModule.forRoot()],
      providers: [
        {
          provide: WalletEntryService,
          useValue: {
            fetchTransferRate,
          },
        },
        {
          provide: GroupService,
          useValue: {
            getAllGroups: vi.fn().mockResolvedValue([]),
          },
        },
        {
          provide: UserService,
          useValue: {
            user: signal({ defaultCurrency: 'USD' } as any),
          },
        },
        {
          provide: CreditCardBillService,
          useValue: {
            getBestBill: vi.fn(),
          },
        },
      ],
    })
      .overrideComponent(TransactionFormComponent, {
        set: {
          template: '',
        },
      })
      .compileComponents();
  });

  it('auto-fills targetValue from stored quote for immediate cross-currency transfer', async () => {
    fetchTransferRate.mockResolvedValue({
      rate: 5.4025,
      quoteDate: '2026-04-10',
      baseCurrency: 'USD',
      quoteCurrency: 'BRL',
    });

    const fixture = TestBed.createComponent(TransactionFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.typeControl.setValue(WalletEntryType__Obj.TRANSFER);
    component.originControl.setValue(walletItem('origin-wallet', 'USD'));
    component.targetControl.setValue(walletItem('target-wallet', 'BRL'));
    component.dateControl.setValue(new Date(2026, 3, 10));
    component.valueControl.setValue(100);

    await new Promise(resolve => setTimeout(resolve, 250));
    await fixture.whenStable();

    expect(fetchTransferRate).toHaveBeenCalledWith({
      groupId: null,
      originId: 'origin-wallet',
      targetId: 'target-wallet',
      date: '2026-04-10',
    });
    expect(component.targetValueControl.value).toBe(540.25);
  });

  it('recomputes target from cached rate when origin amount changes (no extra rate request)', async () => {
    fetchTransferRate.mockResolvedValue({
      rate: 5,
      quoteDate: '2026-04-10',
      baseCurrency: 'USD',
      quoteCurrency: 'BRL',
    });

    const fixture = TestBed.createComponent(TransactionFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.typeControl.setValue(WalletEntryType__Obj.TRANSFER);
    component.originControl.setValue(walletItem('origin-wallet', 'USD'));
    component.targetControl.setValue(walletItem('target-wallet', 'BRL'));
    component.dateControl.setValue(new Date(2026, 3, 10));
    component.valueControl.setValue(100);

    await new Promise(resolve => setTimeout(resolve, 250));
    await fixture.whenStable();

    expect(fetchTransferRate).toHaveBeenCalledTimes(1);

    component.valueControl.setValue(200);
    await fixture.whenStable();

    expect(fetchTransferRate).toHaveBeenCalledTimes(1);
    expect(component.targetValueControl.value).toBe(1000);
  });

  it('requires targetValue for immediate cross-currency transfer when quote suggestion fails', async () => {
    fetchTransferRate.mockRejectedValue(new Error('quote unavailable'));

    const fixture = TestBed.createComponent(TransactionFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.typeControl.setValue(WalletEntryType__Obj.TRANSFER);
    component.originControl.setValue(walletItem('origin-wallet', 'USD'));
    component.targetControl.setValue(walletItem('target-wallet', 'BRL'));
    component.dateControl.setValue(new Date(2026, 3, 10));
    component.valueControl.setValue(100);

    await new Promise(resolve => setTimeout(resolve, 250));
    await fixture.whenStable();

    expect(component.targetValueControl.value).toBeNull();
    expect(component.form.invalid).toBe(true);
    expect(component.transferQuoteError()).toBe('financesPage.transactionsPage.targetValueQuoteUnavailable');
  });

  it('auto-fills and requires targetValue for immediate same-currency transfer', async () => {
    const fixture = TestBed.createComponent(TransactionFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.typeControl.setValue(WalletEntryType__Obj.TRANSFER);
    component.originControl.setValue(walletItem('origin-wallet', 'USD'));
    component.targetControl.setValue(walletItem('target-wallet', 'USD'));
    component.dateControl.setValue(new Date(2026, 3, 10));
    component.valueControl.setValue(100);

    await new Promise(resolve => setTimeout(resolve, 250));
    await fixture.whenStable();

    expect(fetchTransferRate).not.toHaveBeenCalled();
    expect(component.shouldRequireTargetValueField).toBe(true);
    expect(component.targetValueControl.value).toBe(100);
    expect(component.form.valid).toBe(true);

    component.targetValueControl.setValue(null as any);
    await fixture.whenStable();

    expect(component.form.invalid).toBe(true);
  });

  it('preserves manual same-currency target override after auto-fill', async () => {
    const fixture = TestBed.createComponent(TransactionFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.typeControl.setValue(WalletEntryType__Obj.TRANSFER);
    component.originControl.setValue(walletItem('origin-wallet', 'USD'));
    component.targetControl.setValue(walletItem('target-wallet', 'USD'));
    component.dateControl.setValue(new Date(2026, 3, 10));
    component.valueControl.setValue(100);

    await new Promise(resolve => setTimeout(resolve, 250));
    await fixture.whenStable();

    expect(component.targetValueControl.value).toBe(100);

    component.targetValueControl.setValue(95);
    await fixture.whenStable();

    component.valueControl.setValue(120);
    await new Promise(resolve => setTimeout(resolve, 250));
    await fixture.whenStable();

    expect(component.targetValueControl.value).toBe(95);
  });

  it('hides targetValue field while transfer is still only a recurring template', async () => {
    const fixture = TestBed.createComponent(TransactionFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.typeControl.setValue(WalletEntryType__Obj.TRANSFER);
    component.paymentTypeControl.setValue(PaymentType__Obj.RECURRING);

    await fixture.whenStable();

    expect(component.shouldShowTargetValueField).toBe(false);
    expect(component.shouldShowDeferredTargetHint).toBe(true);
  });
});
