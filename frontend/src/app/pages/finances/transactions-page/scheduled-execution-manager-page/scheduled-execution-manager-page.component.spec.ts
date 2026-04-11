import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { Subject } from 'rxjs';

import { MessageService } from 'primeng/api';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { EventForListDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { UserActionEventService } from '../../services/user-action-event.service';
import { WalletEntryService } from '../../services/wallet-entry.service';
import { ScheduledExecutionManagerPageComponent } from './scheduled-execution-manager-page.component';

function buildEntry(paymentType: 'UNIQUE' | 'RECURRING'): EventForListDto {
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
      paymentType,
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

describe('ScheduledExecutionManagerPageComponent', () => {
  const walletEntryServiceMock = {
    listScheduledExecutions: vi.fn().mockResolvedValue([]),
    deleteScheduledEntry: vi.fn().mockResolvedValue(undefined),
  };

  let userActionEventServiceMock: {
    transactionInserted$: Subject<EventForListDto>;
    transactionUpdated$: Subject<EventForListDto>;
    transactionDeleted$: Subject<EventForListDto>;
    resyncRequired$: Subject<void>;
  };

  const errorMessageServiceMock = {
    handleError: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    userActionEventServiceMock = {
      transactionInserted$: new Subject<EventForListDto>(),
      transactionUpdated$: new Subject<EventForListDto>(),
      transactionDeleted$: new Subject<EventForListDto>(),
      resyncRequired$: new Subject<void>(),
    };

    await TestBed.configureTestingModule({
      imports: [ScheduledExecutionManagerPageComponent, TranslateModule.forRoot()],
      providers: [
        { provide: WalletEntryService, useValue: walletEntryServiceMock },
        { provide: UserActionEventService, useValue: userActionEventServiceMock },
        { provide: ErrorMessageService, useValue: errorMessageServiceMock },
        MessageService,
      ],
    }).compileComponents();
  });

  it('builds scheduler edit route links for recurring entries', () => {
    const fixture = TestBed.createComponent(ScheduledExecutionManagerPageComponent);
    const component = fixture.componentInstance;

    expect(component.editRouterLink(buildEntry('RECURRING'))).toEqual(['/app/transactions/scheduler-manager/edit', 'rec-1']);
  });

  it('hides periodicity in recurrenceLabel when paymentType is UNIQUE', () => {
    const fixture = TestBed.createComponent(ScheduledExecutionManagerPageComponent);
    const component = fixture.componentInstance;

    const label = (component as any).recurrenceLabel(buildEntry('UNIQUE')) as string;

    expect(label).toContain('enums.paymentType.UNIQUE');
    expect(label).not.toContain('enums.recurrenceType.');
  });

  it('bypasses scope selection for UNIQUE deletion', async () => {
    const fixture = TestBed.createComponent(ScheduledExecutionManagerPageComponent);
    const component = fixture.componentInstance;

    const uniqueEntry = buildEntry('UNIQUE');
    component.requestDelete(uniqueEntry);
    await fixture.whenStable();

    expect(component.deletingEntry()).toBeNull();
    expect(walletEntryServiceMock.deleteScheduledEntry).toHaveBeenCalledWith('rec-1', {
      occurrenceDate: '2026-02-10',
      scope: undefined,
    });
  });

  it('opens scope selection for non-UNIQUE deletion and supports ALL_SERIES', async () => {
    const fixture = TestBed.createComponent(ScheduledExecutionManagerPageComponent);
    const component = fixture.componentInstance;

    const recurringEntry = buildEntry('RECURRING');
    component.requestDelete(recurringEntry);
    expect(component.deletingEntry()).toEqual(recurringEntry);

    component.chooseDeleteAllSeries();
    await fixture.whenStable();

    expect(walletEntryServiceMock.deleteScheduledEntry).toHaveBeenCalledWith('rec-1', {
      occurrenceDate: '2026-02-10',
      scope: 'ALL_SERIES',
    });
  });

  it('does not call API when delete confirmation is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);

    const fixture = TestBed.createComponent(ScheduledExecutionManagerPageComponent);
    const component = fixture.componentInstance;

    component.requestDelete(buildEntry('UNIQUE'));
    await fixture.whenStable();

    expect(walletEntryServiceMock.deleteScheduledEntry).not.toHaveBeenCalled();
  });

  it('reloads entries when resync-required is emitted', async () => {
    const fixture = TestBed.createComponent(ScheduledExecutionManagerPageComponent);
    const component = fixture.componentInstance;

    await component.ngOnInit();
    walletEntryServiceMock.listScheduledExecutions.mockClear();

    userActionEventServiceMock.resyncRequired$.next();
    await fixture.whenStable();

    expect(walletEntryServiceMock.listScheduledExecutions).toHaveBeenCalledTimes(1);
  });
});
