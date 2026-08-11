import '@angular/compiler';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { describe, expect, it } from 'vitest';

import { FinancesTitleBarComponent } from './finances-title-bar.component';

describe('FinancesTitleBarComponent', () => {
  it('renders a compact badge immediately beside the title', () => {
    TestBed.configureTestingModule({
      imports: [FinancesTitleBarComponent, TranslateModule.forRoot()],
      providers: [provideRouter([])],
    });
    const fixture = TestBed.createComponent(FinancesTitleBarComponent);
    fixture.componentInstance.title = 'Visão geral do grupo: Teste';
    fixture.componentInstance.titleBadge = 'Pro';
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('[data-testid="title-badge"]') as HTMLElement;
    expect(badge.textContent?.trim()).toBe('Pro');
    expect(badge.parentElement?.previousElementSibling?.tagName).toBe('H2');
  });

  it('hides a plan badge when limits are off and still shows an unlimited paid plan name when they are on', () => {
    TestBed.configureTestingModule({
      imports: [PlanAwareTitleBarHost, TranslateModule.forRoot()],
      providers: [provideRouter([])],
    });
    const fixture = TestBed.createComponent(PlanAwareTitleBarHost);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="title-badge"]')).toBeNull();

    fixture.componentInstance.limitsEnabled.set(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="title-badge"]').textContent.trim()).toBe('Pro');
  });
});

@Component({
  imports: [FinancesTitleBarComponent],
  template: '<app-finances-title-bar title="Account" [titleBadge]="limitsEnabled() ? \'Pro\' : undefined" />',
})
class PlanAwareTitleBarHost {
  readonly limitsEnabled = signal(false);
}
