import { TranslateLoader } from '@ngx-translate/core';

import { Observable, of } from 'rxjs';

import bundle from 'virtual:i18n-bundle';

export class CustomTranslateYamlLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<any> {
    return of(bundle[lang] ?? {});
  }
}
