import { TranslateService } from '@ngx-translate/core';

import { Subscription } from 'rxjs';

export const i18nIsReady = async (translateService: TranslateService): Promise<boolean> => {
  return new Promise(resolve => {
    let subscription: Subscription | undefined;

    subscription = translateService.get('title').subscribe(title => {
      if (title != 'title') {
        subscription?.unsubscribe();
        resolve(true);
      }
    });
  });
};
