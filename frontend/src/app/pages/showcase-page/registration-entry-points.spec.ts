import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { EMPTY, Subject, of } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { GithubStarsService } from '../../components/github-stars/github-stars.component';
import { AuthService } from '../../services/auth.service';
import { BreakpointService } from '../../services/breakpoint.service';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';
import { UserService } from '../../services/user.service';
import { ShowcasePageComponent } from './showcase-page.component';

describe('public registration entry points', () => {
  const registrationEnabled = signal(true);

  beforeEach(async () => {
    registrationEnabled.set(true);
    await TestBed.configureTestingModule({
      imports: [ShowcasePageComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: OpenAuthPreferencesService, useValue: { registrationEnabled } },
        { provide: UserService, useValue: { loading: signal(false), user: signal(null) } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
        { provide: BreakpointService, useValue: { isUp: () => signal(false) } },
        { provide: GithubStarsService, useValue: { getStars: () => of(0) } },
        {
          provide: TranslateService,
          useValue: {
            currentLang: 'en-US',
            onLangChange: new Subject(),
            onFallbackLangChange: EMPTY,
            onTranslationChange: EMPTY,
            onDefaultLangChange: EMPTY,
            instant: (key: string) => key,
            get: (key: string) => of(key),
            stream: (key: string) => of(key),
            use: (lang: string) => of(lang),
          },
        },
      ],
    }).compileComponents();
  });

  it('shows registration actions in the landing page and navbar when enabled', () => {
    const fixture = TestBed.createComponent(ShowcasePageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('a[href="/register"]')).toHaveLength(2);
  });

  it('hides registration actions in the landing page and navbar when disabled', () => {
    registrationEnabled.set(false);
    const fixture = TestBed.createComponent(ShowcasePageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('a[href="/register"]')).toHaveLength(0);
  });
});
