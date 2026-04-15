import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { OpenAuthPreferencesDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';

/** Defaults when the request fails (aligned with backend defaults). */
const FALLBACK: OpenAuthPreferencesDto = {
  emailConfirmationEnabled: true,
  passwordRecoveryEnabled: true,
  turnstileEnabled: true,
};

/**
 * Loads `/api/open/auth/preferences` once and shares feature flags across the app.
 */
@Injectable({ providedIn: 'root' })
export class OpenAuthPreferencesService {
  private readonly http = inject(HttpClient);

  /** Set after the first successful or failed load attempt. */
  readonly preferences = signal<OpenAuthPreferencesDto | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal<unknown>(null);

  readonly emailConfirmationEnabled = computed(() => this.preferences()?.emailConfirmationEnabled ?? FALLBACK.emailConfirmationEnabled);
  readonly passwordRecoveryEnabled = computed(() => this.preferences()?.passwordRecoveryEnabled ?? FALLBACK.passwordRecoveryEnabled);
  readonly turnstileEnabled = computed(() => this.preferences()?.turnstileEnabled ?? FALLBACK.turnstileEnabled);

  private loadPromise: Promise<void> | null = null;

  /**
   * Idempotent: concurrent callers await the same in-flight request; later calls resolve immediately.
   */
  load(): Promise<void> {
    if (this.loadPromise) {
      return this.loadPromise;
    }
    this.loadPromise = this.doLoad();
    return this.loadPromise;
  }

  private async doLoad(): Promise<void> {
    this.loading.set(true);
    try {
      const dto = await lastValueFrom(this.http.get<OpenAuthPreferencesDto>('/api/open/auth/preferences').pipe(take(1)));
      this.preferences.set(dto);
      this.loadError.set(null);
    } catch (e) {
      this.loadError.set(e);
      this.preferences.set(FALLBACK);
    } finally {
      this.loading.set(false);
    }
  }
}
