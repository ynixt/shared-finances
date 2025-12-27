import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { lastValueFrom, take } from 'rxjs';

import { authGuard } from '../guards/auth.guard';
import { LoginDto, RegisterDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { AuthHttpService } from './auth-http.service';
import { GuardInspector } from './guard-inspector.service';
import { TokenStateService } from './token-state.service';
import { TokenSyncService } from './token-sync.service';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
@UntilDestroy()
export class AuthService {
  private firstUserLoad = true;

  constructor(
    private router: Router,
    private activeRoute: ActivatedRoute,
    private guardInspector: GuardInspector,
    private userService: UserService,
    private tokenSyncService: TokenSyncService,
    private activatedRoute: ActivatedRoute,
    private authHttpService: AuthHttpService,
    private tokenStateService: TokenStateService,
  ) {
    this.tokenStateService.token$.pipe(untilDestroyed(this)).subscribe(token => {
      const currentUser = this.userService.user();

      // current user can be different from null even if firstUserLoad is false, because user can be received from another browser tab
      if (!currentUser && this.firstUserLoad) {
        this.firstUserLoad = false;
        this.loadUser();
      }
    });

    this.tokenSyncService.onLoginMessage.pipe(untilDestroyed(this)).subscribe(msg => {
      this.userService.changeUser(msg.user);
      setTimeout(() => {
        this.tokenStateService.changeToken(msg.token);
        this.navigateAfterLoginSuccess();
      }, 0);
    });

    this.tokenSyncService.onLogoutMessage.pipe(untilDestroyed(this)).subscribe(async msg => {
      let returnTo = this.activatedRoute.snapshot.queryParamMap.get('return_to');

      if (returnTo == null && this.router.url != '/login' && this.router.url != '/app') {
        returnTo = this.router.url;
      }

      await this.logout({
        returnTo: returnTo == null ? undefined : returnTo,
        callHttpLogout: false,
      });
    });

    this.tokenSyncService.newTokenMessage.pipe(untilDestroyed(this)).subscribe(msg => {
      this.tokenStateService.changeToken(msg.token);
    });
  }

  private async changeTokenAndSync(token: string | undefined | null) {
    this.tokenStateService.changeToken(token);

    if (token != null) {
      await this.loadUser();
      this.tokenSyncService.postTokenUpdatedMessage(token);
    } else {
      this.tokenSyncService.postLogoutMessage();
    }
  }

  private async loadUser() {
    try {
      const user = await this.getUserFromHttp();
      this.userService.changeUser(user);
    } catch (err) {
      this.userService.changeUser(null, err);
      this.logout({ ignoreError: true, sync: false });
    }
  }

  private async getUserFromHttp(retryOnError = true): Promise<UserResponseDto> {
    let token = await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));

    if (token == null) throw 'Missing token.';

    try {
      return await this.authHttpService.getUser();
    } catch (err) {
      if (err instanceof HttpErrorResponse && err.status === 401 && retryOnError) {
        const newToken = await this.refreshJwt();

        if (newToken != null) {
          return this.getUserFromHttp(false);
        }
      }
      throw err;
    }
  }

  async submitLogin(body: LoginDto): Promise<any> {
    try {
      this.userService.loading.set(true);
      const response = await this.authHttpService.login(body);
      const token = this.getTokenFromHeaders(response.headers);
      await this.changeTokenAndSync(token);
    } catch (err) {
      this.userService.loading.set(false);
      throw err;
    }

    return await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));
  }

  async submitRegistration(body: RegisterDto): Promise<object> {
    return this.authHttpService.register(body);
  }

  async logout(args?: { returnTo?: string | undefined; ignoreError?: boolean; sync?: boolean; callHttpLogout?: boolean }): Promise<void> {
    if (!args) args = {};

    args.sync = args.sync ?? true;
    args.callHttpLogout = args.callHttpLogout ?? true;

    const token = await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));

    if (token != null && args.callHttpLogout) {
      try {
        await this.authHttpService.logout();
      } catch (err) {
        if (!args.ignoreError) throw err;
      }

      if (args.sync) {
        await this.changeTokenAndSync(null);
      } else {
        this.tokenStateService.changeToken(token);
      }
    }

    this.userService.changeUser(null);

    if (this.guardInspector.hasCanActivateInHierarchy(authGuard)) {
      setTimeout(async () => {
        await this.navigateAfterLogoutSuccess(args.returnTo);
      }, 0);
    }
  }

  async waitForLoginSuccess() {
    const user = await this.userService.getUser();
    const token = await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));

    this.tokenSyncService.newLogin(user!!, token!!);
    this.navigateAfterLoginSuccess();
  }

  private async refreshJwt(): Promise<string | null> {
    try {
      const response = await this.authHttpService.refreshJwt();
      const token = this.getTokenFromHeaders(response.headers);
      await this.changeTokenAndSync(token);
    } catch (err) {
      await this.logout({
        sync: true,
        callHttpLogout: false,
        returnTo: window.location.pathname,
      });
      throw err;
    }

    return await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));
  }

  private getTokenFromHeaders(headers: HttpHeaders): string | null {
    const value = headers.get('Authorization');

    if (value) return value.replace('Bearer ', '');
    return null;
  }

  private navigateAfterLoginSuccess() {
    return this.router.navigateByUrl(this.activeRoute.snapshot.queryParamMap.get('return_to') ?? '/app');
  }

  private async navigateAfterLogoutSuccess(return_to: string | undefined) {
    await this.router.navigate(['/login'], {
      onSameUrlNavigation: 'reload',
      queryParams: {
        return_to,
      },
    });
  }
}
