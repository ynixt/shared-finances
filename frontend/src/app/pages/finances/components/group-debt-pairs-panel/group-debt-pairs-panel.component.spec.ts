import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { ConfirmationService } from 'primeng/api';
import { describe, expect, it, vi } from 'vitest';

import { GroupOverviewDebtPairDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/dashboard';
import { GroupDebtPairsPanelComponent } from './group-debt-pairs-panel.component';

describe('GroupDebtPairsPanelComponent', () => {
  it('renders an absent participant name through translation and keeps departed pairs distinct', () => {
    TestBed.configureTestingModule({
      imports: [GroupDebtPairsPanelComponent],
      providers: [{ provide: TranslateService, useValue: { instant: vi.fn().mockReturnValue('Unknown user') } }],
    });
    TestBed.overrideComponent(GroupDebtPairsPanelComponent, {
      set: { template: '', providers: [{ provide: ConfirmationService, useValue: { confirm: vi.fn() } }] },
    });
    const component = TestBed.createComponent(GroupDebtPairsPanelComponent).componentInstance;

    expect(component.participantName(null)).toBe('Unknown user');
    expect(component.trackDebtPair(0, pair('a', 'b'))).not.toBe(component.trackDebtPair(1, pair('c', 'b')));
  });
});

function pair(payerId: string, receiverId: string): GroupOverviewDebtPairDto {
  return {
    payerId,
    receiverId,
    currency: 'BRL',
    outstandingAmount: 10,
    details: [],
  };
}
