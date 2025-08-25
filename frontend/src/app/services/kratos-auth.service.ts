import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Configuration, FrontendApi } from '@ory/client';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KratosAuthService {
  private readonly kratos = environment.kratosPublicUrl;

  readonly token = signal<string | null>(null);

  readonly kratosApi = new FrontendApi(
    new Configuration({
      basePath: this.kratos,
      baseOptions: {
        withCredentials: true,
      },
    }),
  );

  constructor(private router: Router) {}

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
        ...body,
      })
    ).data;
  }

  async logout(): Promise<void> {
    const { data: flow } = await this.kratosApi.createBrowserLogoutFlow();
    await this.kratosApi.updateLogoutFlow({
      token: flow.logout_token,
    });
    this.token.set(null);
    await this.router.navigateByUrl('/login', {
      onSameUrlNavigation: 'reload',
    });
  }

  async refreshJwt(): Promise<void> {
    try {
      const { tokenized } = (
        await this.kratosApi.toSession({
          tokenizeAs: 'default_jwt',
        })
      ).data;

      this.token.set(tokenized ?? null);
    } catch (err) {
      this.token.set(null);
      throw err;
    }
  }

  async getToken(): Promise<string | null> {
    const current = this.token();
    if (current != null) {
      return current;
    }
    await this.refreshJwt();
    return this.token();
  }
}
