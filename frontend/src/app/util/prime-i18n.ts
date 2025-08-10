import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';

import { lastValueFrom, take } from 'rxjs';

import { PrimeNG } from 'primeng/config';

import { environment } from '../../environments/environment';
import { i18nIsReady } from './i18n-util';

const primeI18nDict = new Map<string, any>();

export const updatePrimeI18n = async (primengConfig: PrimeNG, translateService: TranslateService, httpClient: HttpClient) => {
  await i18nIsReady(translateService);

  let currentLang = translateService.currentLang;

  if (currentLang == null) return;

  if (currentLang == 'en-US') {
    currentLang = 'en';
  }

  let translationObj: any;

  try {
    if (primeI18nDict.has(currentLang)) {
      translationObj = currentLang;
    } else {
      translationObj = await lastValueFrom(httpClient.get(`/public/prime-i18n/${currentLang}.json`).pipe(take(1)));
      primeI18nDict.set(currentLang, translationObj);
    }
  } catch (err) {
    if (primeI18nDict.has(environment.defaultPrimeLanguage)) {
      translationObj = environment.defaultPrimeLanguage;
    } else {
      translationObj = await lastValueFrom(httpClient.get(`/public/prime-i18n/${environment.defaultPrimeLanguage}.json`).pipe(take(1)));
      primeI18nDict.set(environment.defaultPrimeLanguage, translationObj);
    }
  }

  if (translationObj) {
    primengConfig.setTranslation(translationObj[Object.keys(translationObj)[0]]);
  }
};
