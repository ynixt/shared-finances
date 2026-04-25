import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { lastValueFrom, take } from 'rxjs';

import { BroadcastChannel, type LeaderElector, createLeaderElection } from 'broadcast-channel';

import { getDefaultCategoriesTranslated } from '../../default-categories';
import { UserOnboardingDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { CategoryConceptDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';

@Injectable({ providedIn: 'root' })
export class OnboardingService implements OnDestroy {
  private static readonly channelName = 'sf-onboarding-v1';

  private channel: BroadcastChannel<object> | null = null;
  private elector: LeaderElector | null = null;
  private destroyed = false;

  constructor(
    private http: HttpClient,
    private translateService: TranslateService,
  ) {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      this.channel = new BroadcastChannel<object>(OnboardingService.channelName);
      this.elector = createLeaderElection(this.channel);
      this.elector.onduplicate = () => {
        console.warn('[onboarding] duplicate leader detected');
      };
    } catch (error) {
      console.warn('[onboarding] unable to initialize leader election', error);
      void this.channel?.close().catch(() => undefined);
      this.channel = null;
      this.elector = null;
    }
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    const channel = this.channel;
    const elector = this.elector;

    this.channel = null;
    this.elector = null;

    void this.dispose(channel, elector);
  }

  async onboarding(): Promise<boolean> {
    const shouldRunInThisTab = await this.shouldRunOnboardingInThisTab();

    if (!shouldRunInThisTab) {
      return false;
    }

    await this.buildOnboardingRequestBody().then(requestBody =>
      lastValueFrom(this.http.post<void>('/api/users/current/onboarding', requestBody).pipe(take(1))),
    );

    return true;
  }

  private async buildOnboardingRequestBody(): Promise<UserOnboardingDto> {
    const concepts = await lastValueFrom(this.http.get<CategoryConceptDto[]>('/api/categories/concepts').pipe(take(1)));
    const translatedCategories = getDefaultCategoriesTranslated(this.translateService, concepts);

    return {
      categories: translatedCategories,
    };
  }

  private async shouldRunOnboardingInThisTab(): Promise<boolean> {
    const elector = this.elector;

    if (elector == null) {
      return true;
    }

    if (elector.isLeader) {
      return true;
    }

    try {
      await elector.awaitLeadership();
      return !this.destroyed;
    } catch (error) {
      if (!this.destroyed) {
        console.warn('[onboarding] leader election failed', error);
      }
      return true;
    }
  }

  private async dispose(channel: BroadcastChannel<object> | null, elector: LeaderElector | null): Promise<void> {
    try {
      await elector?.die();
    } catch {
      // Ignore teardown failures during tab close/unload.
    }

    try {
      await channel?.close();
    } catch {
      // Ignore teardown failures during tab close/unload.
    }
  }
}
