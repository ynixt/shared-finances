import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Configuration, FrontendApi } from '@ory/client';

import { BehaviorSubject, filter, lastValueFrom, take } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KratosAuthService {
  private readonly kratos = environment.kratosPublicUrl;

  readonly tokenSubject = new BehaviorSubject<string | null | undefined>(undefined);
  readonly token$ = this.tokenSubject.pipe(filter(t => t !== undefined));

  readonly kratosApi = new FrontendApi(
    new Configuration({
      basePath: this.kratos,
      baseOptions: {
        withCredentials: true,
      },
    }),
  );

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
  ) {}

  async getLoginFlow(returnTo = '/'): Promise<any> {
    const response = await this.kratosApi.createBrowserLoginFlow({
      returnTo,
    });

    return response.data;
  }

  async getRegistrationFlow(returnTo = '/login'): Promise<any> {
    return (
      await this.kratosApi.createBrowserRegistrationFlow({
        returnTo,
      })
    ).data;
  }

  async submitLoginFlow(flowId: string, body: any): Promise<any> {
    const response = await this.kratosApi.updateLoginFlow({
      flow: flowId,
      updateLoginFlowBody: {
        method: 'password',
        ...body,
      },
    });

    return response.data;
  }

  async submitRegistrationFlow(flowId: string, body: any): Promise<object> {
    return (
      await this.kratosApi.updateRegistrationFlow({
        flow: flowId,
        updateRegistrationFlowBody: body,
      })
    ).data;
  }

  async logout(args?: { alsoLogoutKratos?: boolean; returnTo?: string | undefined }): Promise<void> {
    if (!args) args = {};
    // console.log('ads')

    args.alsoLogoutKratos ??= true;

    if (args.alsoLogoutKratos) {
      const { data: flow } = await this.kratosApi.createBrowserLogoutFlow();
      await this.kratosApi.updateLogoutFlow({
        token: flow.logout_token,
      });
    }

    this.tokenSubject.next(null);
    if (this.router.url != '/register') {
      await this.router.navigate(['/login'], {
        onSameUrlNavigation: 'reload',
        queryParams: {
          return_to: args.returnTo,
        },
      });
    }
  }

  loginSuccess() {
    return this.router.navigateByUrl(this.activeRoute.snapshot.queryParamMap.get('return_to') ?? '/app');
  }

  async refreshJwt(): Promise<string | null> {
    try {
      const { tokenized } = (
        await this.kratosApi.toSession({
          tokenizeAs: 'default_jwt',
        })
      ).data;

      this.tokenSubject.next(tokenized ?? null);
    } catch (err) {
      this.tokenSubject.next(null);
      throw err;
    }

    return await lastValueFrom(this.token$.pipe(take(1)));
  }
}
