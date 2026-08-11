import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { firstValueFrom } from 'rxjs';

import { describe, expect, it } from 'vitest';

import { PlanIdentityComponent } from './plan-identity.component';

describe('PlanIdentityComponent', () => {
  it.each([
    ['USER', 'Common'],
    ['PRO', 'Pro'],
    ['ADMINISTRATOR', 'Administrator'],
  ])('renders user plan %s by its translated name', async (value, translatedName) => {
    const text = await render('user', value);
    expect(text).toContain(`Your plan: ${translatedName}`);
    expect(text).not.toContain(value);
  });

  it.each([
    ['COMMON', 'Common'],
    ['PRO', 'Pro'],
  ])('renders group tier %s by its translated name and owner attribution', async (value, translatedName) => {
    const text = await render('group', value);
    expect(text).toContain(`Group tier: ${translatedName}`);
    expect(text).toContain("determined by the group's owner");
    expect(text).not.toContain(value);
  });
});

async function render(kind: 'user' | 'group', value: string): Promise<string> {
  TestBed.configureTestingModule({ imports: [PlanIdentityComponent, TranslateModule.forRoot()] });
  const translate = TestBed.inject(TranslateService);
  translate.setTranslation('en-US', {
    planIdentity: {
      userPlan: 'Your plan',
      groupTier: 'Group tier',
      ownerDeterminesTier: "determined by the group's owner",
      userPlans: { USER: 'Common', PRO: 'Pro', ADMINISTRATOR: 'Administrator' },
      groupTiers: { COMMON: 'Common', PRO: 'Pro' },
    },
  });
  await firstValueFrom(translate.use('en-US'));

  const fixture = TestBed.createComponent(PlanIdentityComponent);
  fixture.componentRef.setInput('kind', kind);
  fixture.componentRef.setInput('value', value);
  fixture.detectChanges();
  return (fixture.nativeElement.textContent as string).replace(/\s+/g, ' ').trim();
}
