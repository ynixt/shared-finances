import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { firstValueFrom } from 'rxjs';

import { beforeEach, describe, expect, it } from 'vitest';

import { AccountActivityInfoComponent } from './account-activity-info.component';

describe('AccountActivityInfoComponent', () => {
  beforeEach(async () => {
    TestBed.configureTestingModule({ imports: [AccountActivityInfoComponent, TranslateModule.forRoot()] });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('en-US', {
      financesPage: {
        userSettingsPage: {
          lastUse: 'Last use',
          projectedDeletion: 'Projected deletion',
          projectedDeletionExplanation: 'Signing in resets this date.',
        },
      },
    });
    await firstValueFrom(translate.use('en-US'));
  });

  it('shows last use and projected deletion for a plan that expires', () => {
    const element = render('2026-01-01T12:00:00Z', '2027-01-01T12:00:00Z');
    expect(element.querySelector('[data-testid="last-use"]')?.textContent).toBeTruthy();
    expect(element.querySelector('[data-testid="projected-deletion"]')?.textContent).toContain('Signing in resets this date.');
  });

  it.each(['plan without a retention period', 'instance with the feature switched off'])('shows no projected deletion for an %s', () => {
    const element = render('2026-01-01T12:00:00Z', null);
    expect(element.querySelector('[data-testid="last-use"]')).not.toBeNull();
    expect(element.querySelector('[data-testid="projected-deletion"]')).toBeNull();
  });
});

function render(lastLoginAt: string, projectedDeletionAt: string | null): HTMLElement {
  const fixture = TestBed.createComponent(AccountActivityInfoComponent);
  fixture.componentRef.setInput('lastLoginAt', lastLoginAt);
  fixture.componentRef.setInput('projectedDeletionAt', projectedDeletionAt);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}
