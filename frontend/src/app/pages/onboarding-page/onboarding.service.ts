import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { lastValueFrom, take } from 'rxjs';

import { getDefaultCategoriesTranslated } from '../../default-categories';

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  constructor(
    private http: HttpClient,
    private translateService: TranslateService,
  ) {}

  createDefaultCategories() {
    const translatedCategories = getDefaultCategoriesTranslated(this.translateService);

    return lastValueFrom(this.http.post<void>('/api/categories/bulk', translatedCategories).pipe(take(1)));
  }
}
