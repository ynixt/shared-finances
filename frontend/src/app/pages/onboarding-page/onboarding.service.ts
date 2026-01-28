import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { lastValueFrom, take } from 'rxjs';

import { getDefaultCategoriesTranslated } from '../../default-categories';
import { UserOnboardingDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  constructor(
    private http: HttpClient,
    private translateService: TranslateService,
  ) {}

  onboarding() {
    const translatedCategories = getDefaultCategoriesTranslated(this.translateService);

    const requestBody: UserOnboardingDto = {
      categories: translatedCategories,
    };

    return lastValueFrom(this.http.post<void>('/api/users/current/onboarding', requestBody).pipe(take(1)));
  }
}
