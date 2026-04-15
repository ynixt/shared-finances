import { AfterViewInit, Component, ElementRef, EventEmitter, OnDestroy, Output, ViewChild, inject, input, signal } from '@angular/core';

import { environment } from '../../../environments/environment';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';

declare global {
  interface Window {
    turnstile?: {
      render: (container: HTMLElement | string, params: Record<string, unknown>) => string;
      reset?: (widgetId: string) => void;
      remove?: (widgetId: string) => void;
    };
  }
}

function loadTurnstileScript(): Promise<void> {
  if (typeof window.turnstile !== 'undefined') {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-turnstile-api]');
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', () => reject(new Error('Turnstile script load failed')));
      return;
    }
    const s = document.createElement('script');
    s.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    s.async = true;
    s.defer = true;
    s.setAttribute('data-turnstile-api', 'true');
    s.onload = () => resolve();
    s.onerror = () => reject(new Error('Turnstile script load failed'));
    document.head.appendChild(s);
  });
}

@Component({
  selector: 'app-turnstile-widget',
  templateUrl: './turnstile-widget.component.html',
  styleUrl: './turnstile-widget.component.scss',
})
export class TurnstileWidgetComponent implements AfterViewInit, OnDestroy {
  @ViewChild('host') host!: ElementRef<HTMLDivElement>;

  private readonly openAuthPreferences = inject(OpenAuthPreferencesService);

  show = signal(false);
  siteKey = input<string | undefined>(environment.turnstileSiteKey ?? '');

  @Output() resolved = new EventEmitter<string | null>();

  private widgetId?: string;

  async ngAfterViewInit(): Promise<void> {
    await this.openAuthPreferences.load();

    const enabled = this.openAuthPreferences.turnstileEnabled();
    this.show.set(enabled);

    if (!enabled) {
      this.resolved.emit(null);
      return;
    }

    if (!this.siteKey()) {
      this.resolved.emit(null);
      return;
    }

    await loadTurnstileScript();

    const tw = window.turnstile;

    if (!tw) {
      this.resolved.emit(null);
      return;
    }

    this.widgetId = tw.render(this.host.nativeElement, {
      sitekey: this.siteKey(),
      callback: (token: string) => this.resolved.emit(token),
      'expired-callback': () => this.resolved.emit(null),
      'error-callback': () => this.resolved.emit(null),
    });
  }

  ngOnDestroy(): void {
    if (this.widgetId && window.turnstile?.remove) {
      window.turnstile.remove(this.widgetId);
    }
  }

  reset(): void {
    if (this.widgetId && window.turnstile?.reset) {
      window.turnstile.reset(this.widgetId);
    }
    this.resolved.emit(null);
  }
}
