import { HttpErrorResponse } from '@angular/common/http';
import '@angular/compiler';
import { Component, input, output } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { BehaviorSubject } from 'rxjs';

import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { EventForListDto, NewEntryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { WalletEntryService } from '../../services/wallet-entry.service';
import { TransactionFormComponent } from '../shared/transaction-form/transaction-form.component';
import { EditScheduledTransactionPageComponent } from './edit-scheduled-transaction-page.component';

@Component({
  selector: 'app-finances-title-bar',
  standalone: true,
  template: '',
})
class FinancesTitleBarStubComponent {
  readonly title = input('');
  readonly closeRouterLink = input<string | string[] | undefined>(undefined);
}

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  template: '',
})
class TransactionFormStubComponent {
  readonly mode = input<'create' | 'edit'>('create');
  readonly submitting = input(false);
  readonly initialEntry = input<any>(undefined);
  readonly withFuture = input(false);
  readonly formSubmitted = output<NewEntryDto>();
}

function buildEntry(): EventForListDto {
  return {
    id: null,
    type: 'EXPENSE',
    name: 'Scheduled expense',
    category: null,
    user: null,
    group: null,
    tags: [],
    observations: null,
    date: '2026-02-10',
    confirmed: false,
    installment: null,
    recurrenceConfigId: 'rec-1',
    recurrenceConfig: {
      id: 'rec-1',
      paymentType: 'RECURRING',
      periodicity: 'MONTHLY',
      qtyExecuted: 0,
      qtyLimit: 12,
      lastExecution: null,
      nextExecution: '2026-02-10',
      endExecution: null,
    },
    currency: 'BRL',
    entries: [
      {
        value: -100,
        walletItemId: 'wallet-1',
        billDate: null,
        billId: null,
        walletItem: {
          id: 'wallet-1',
          name: 'Card',
          type: 'CREDIT_CARD',
          enabled: true,
          currency: 'BRL',
          showOnDashboard: true,
          dueDay: 10,
          daysBetweenDueAndClosing: 7,
          dueOnNextBusinessDay: false,
          user: null,
        },
      },
    ],
  };
}

describe('EditScheduledTransactionPageComponent', () => {
  const getScheduledEntryByRecurrenceConfigId = vi.fn().mockResolvedValue(buildEntry());
  const editScheduledEntry = vi.fn().mockResolvedValue(undefined);

  const walletEntryServiceMock = {
    getScheduledEntryByRecurrenceConfigId,
    editScheduledEntry,
  };

  const routerMock = {
    navigate: vi.fn().mockResolvedValue(true),
    navigateByUrl: vi.fn().mockResolvedValue(true),
  };

  const errorMessageServiceMock = {
    handleError: vi.fn(),
  };

  const paramMap$ = new BehaviorSubject(convertToParamMap({ recurrenceConfigId: 'rec-1' }));
  const queryParamMap$ = new BehaviorSubject(convertToParamMap({}));
  const activatedRouteMock = {
    paramMap: paramMap$.asObservable(),
    queryParamMap: queryParamMap$.asObservable(),
    snapshot: {
      paramMap: paramMap$.value,
      queryParamMap: queryParamMap$.value,
    },
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    paramMap$.next(convertToParamMap({ recurrenceConfigId: 'rec-1' }));
    queryParamMap$.next(convertToParamMap({}));
    activatedRouteMock.snapshot.paramMap = paramMap$.value;
    activatedRouteMock.snapshot.queryParamMap = queryParamMap$.value;

    await TestBed.configureTestingModule({
      imports: [EditScheduledTransactionPageComponent, TranslateModule.forRoot()],
      providers: [
        { provide: WalletEntryService, useValue: walletEntryServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ActivatedRoute, useValue: activatedRouteMock },
        { provide: ErrorMessageService, useValue: errorMessageServiceMock },
        MessageService,
      ],
    })
      .overrideComponent(EditScheduledTransactionPageComponent, {
        remove: {
          imports: [FinancesTitleBarComponent, TransactionFormComponent],
        },
        add: {
          imports: [FinancesTitleBarStubComponent, TransactionFormStubComponent],
        },
      })
      .compileComponents();
  });

  it('loads scheduled entry by recurrenceConfigId and renders shared transaction form', async () => {
    const fixture = TestBed.createComponent(EditScheduledTransactionPageComponent);

    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(getScheduledEntryByRecurrenceConfigId).toHaveBeenCalledWith('rec-1');
    expect(fixture.nativeElement.querySelector('app-transaction-form')).not.toBeNull();
  });

  it('parses withFuture query param when present', async () => {
    queryParamMap$.next(convertToParamMap({ withFuture: 'true' }));
    activatedRouteMock.snapshot.queryParamMap = queryParamMap$.value;

    const fixture = TestBed.createComponent(EditScheduledTransactionPageComponent);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.withFuture()).toBe(true);
  });

  it('submits scheduled edit using THIS_AND_FUTURE when withFuture=true', async () => {
    const fixture = TestBed.createComponent(EditScheduledTransactionPageComponent);
    const component = fixture.componentInstance;

    component.entry.set(buildEntry());
    component.withFuture.set(true);

    const payload: NewEntryDto = {
      type: 'EXPENSE',
      originId: 'wallet-1',
      targetId: undefined,
      groupId: undefined,
      name: 'Edited',
      categoryId: undefined,
      date: '2026-02-10',
      value: 100,
      confirmed: true,
      observations: undefined,
      paymentType: 'RECURRING',
      installments: undefined,
      periodicity: 'MONTHLY',
      periodicityQtyLimit: 12,
      originBillDate: undefined,
      targetBillDate: undefined,
      tags: undefined,
    };

    await component.submit(payload);

    expect(editScheduledEntry).toHaveBeenCalledWith(
      'rec-1',
      expect.objectContaining({
        occurrenceDate: '2026-02-10',
        scope: 'THIS_AND_FUTURE',
      }),
    );
  });

  it('navigates to not-found when scheduled endpoint returns 404', async () => {
    getScheduledEntryByRecurrenceConfigId.mockRejectedValueOnce(new HttpErrorResponse({ status: 404 }));

    const fixture = TestBed.createComponent(EditScheduledTransactionPageComponent);

    fixture.detectChanges();
    await fixture.whenStable();

    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/not-found');
  });
});
