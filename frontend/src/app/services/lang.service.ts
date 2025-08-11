import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, signal } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { PrimeNG } from 'primeng/config';

import { environment } from '../../environments/environment';
import { i18nIsReady } from '../util/i18n-util';
import { updatePrimeI18n } from '../util/prime-i18n';
import { UserService } from './user.service';

@Injectable({
  providedIn: 'root',
})
@UntilDestroy()
export class LangService {
  readonly mapLanguagesNames: { [key: string]: string } = {
    'en-US': 'English',
    'pt-BR': 'Português',
  };
  readonly languages: string[] = Object.keys(this.mapLanguagesNames);

  private _currentLang = signal(environment.defaultLanguage);

  currentLang = this._currentLang.asReadonly();

  constructor(
    private primengConfig: PrimeNG,
    private translateService: TranslateService,
    private httpClient: HttpClient,
    private userService: UserService,
  ) {
    this.init();
    effect(() => {
      this.changeLanguage(this.getBestLangForBrowser());
    });
  }

  async getAllLanguages(): Promise<{ value: string; name: string; current: boolean }[]> {
    await i18nIsReady(this.translateService);

    return this.languages
      .map(langCode => ({
        value: langCode,
        name: this.mapLanguagesNames[langCode],
        current: langCode === this._currentLang(),
      }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  changeLanguage(newLanguage: string) {
    this._currentLang.set(newLanguage);
    this.translateService.use(newLanguage);
    updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);
  }

  private async init() {
    this.changeLanguage(this.getBestLangForBrowser());

    await i18nIsReady(this.translateService);
  }

  private getBestLangForBrowser(): string {
    const user = this.userService.user();

    if (user != null) {
      return user.lang;
    }

    const browserLanguages = navigator?.languages ?? [];

    for (const browserLang of browserLanguages) {
      if (this.hasLanguage(browserLang)) {
        return browserLang;
      }
    }

    return environment.defaultLanguage;
  }

  private hasLanguage(lang: string): boolean {
    for (const lang of this.languages) {
      if (lang.split('-')[0] === lang.split('-')[0]) {
        return true;
      }
    }

    return false;
  }
}
