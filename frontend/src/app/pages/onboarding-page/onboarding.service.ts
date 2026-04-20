import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { lastValueFrom, take } from 'rxjs';

import { getDefaultCategoriesTranslated } from '../../default-categories';
import { UserOnboardingDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { CategoryConceptDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  constructor(
    private http: HttpClient,
    private translateService: TranslateService,
  ) {}

  onboarding() {
    return this.buildOnboardingRequestBody().then(requestBody =>
      lastValueFrom(this.http.post<void>('/api/users/current/onboarding', requestBody).pipe(take(1))),
    );
  }

  private async buildOnboardingRequestBody(): Promise<UserOnboardingDto> {
    const concepts = await lastValueFrom(this.http.get<CategoryConceptDto[]>('/api/categories/concepts').pipe(take(1)));
    const translatedCategories = getDefaultCategoriesTranslated(this.translateService, concepts);

    return {
      categories: translatedCategories,
    };
  }
}
