import { HttpBackend, HttpClient } from '@angular/common/http';
import { TranslateLoader } from '@ngx-translate/core';

import { BehaviorSubject, Observable, catchError, forkJoin, map, of } from 'rxjs';

import { deepmerge } from 'deepmerge-ts';
import { parse as parseYaml } from 'yaml';

export class TranslationLoaderObserver {
  private static _instance: TranslationLoaderObserver | undefined;

  static get instance(): TranslationLoaderObserver {
    if (this._instance == null) {
      this._instance = new TranslationLoaderObserver();
    }

    return this._instance;
  }

  loaded = new BehaviorSubject(false);
}

export interface ITranslationResource {
  prefix: string;
  suffix?: string;
}

export class CustomTranslateHttpLoader implements TranslateLoader {
  constructor(
    private defaultSuffix: string,
    private handler: HttpBackend,
    private resourcesPrefix: string[] | ITranslationResource[],
  ) {}

  public getTranslation(lang: string): Observable<any> {
    const requests: Observable<object>[] = this.resourcesPrefix.map(resource => {
      let path: string;
      if (typeof resource === 'object' && 'prefix' in resource) {
        path = `${resource.prefix}${lang}${resource.suffix || this.defaultSuffix}`;
      } else {
        path = `${resource}${lang}${this.defaultSuffix}`;
      }

      return new HttpClient(this.handler)
        .get(path, {
          responseType: 'text',
        })
        .pipe(
          map(response => parseYaml(response)),
          catchError(res => {
            return of({});
          }),
        );
    });

    return forkJoin(requests).pipe(
      map(response => {
        TranslationLoaderObserver.instance.loaded.next(true);
        return deepmerge(...response);
      }),
    );
  }
}
