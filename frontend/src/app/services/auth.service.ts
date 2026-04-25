import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';

import { Subject, lastValueFrom, take } from 'rxjs';

import { authGuard } from '../guards/auth.guard';
import {
  LoginDto,
  LoginMfaDto,
  LoginResultDto,
  RegisterDto,
  RegisterResultDto,
} from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { AuthHttpService } from './auth-http.service';
import { GuardInspector } from './guard-inspector.service';
import { TokenStateService } from './token-state.service';
import { TokenSyncService } from './token-sync.service';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
@UntilDestroy()
export class AuthService {
  onServerOffline$ = new Subject<void>();
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
      if (currentUser != null || !this.firstUserLoad) {
        return;
      }

      this.firstUserLoad = false;

      if (token == null) {
        this.userService.changeUser(null);
        return;
      }

      void this.loadUser().catch(() => undefined);
    });

    this.tokenSyncService.onLoginMessage.pipe(untilDestroyed(this)).subscribe(msg => {
      this.userService.changeUser(msg.user);
      setTimeout(() => {
        this.tokenStateService.changeToken(msg.token);
        this.navigateAfterLoginSuccess();
      }, 0);
    });

    this.tokenSyncService.onLogoutMessage.pipe(untilDestroyed(this)).subscribe(async _msg => {
      const returnTo = this.activatedRoute.snapshot.queryParamMap.get('return_to') ?? this.resolveCurrentProtectedReturnTo();

      await this.logout({
        returnTo: returnTo == null ? undefined : returnTo,
        sync: false,
        callHttpLogout: false,
      });
    });

    this.tokenSyncService.newTokenMessage.pipe(untilDestroyed(this)).subscribe(msg => {
      this.tokenStateService.changeToken(msg.token);
    });
  }

  async loadUser() {
    try {
      const user = await this.getUserFromHttp();
      this.userService.changeUser(user);
    } catch (err) {
      if (err instanceof HttpErrorResponse) {
        if (err.status < 400 || err.status > 499) {
          this.onServerOffline$.next();
          return;
        }
      }

      this.userService.changeUser(null, err);
      const token = await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));
      if (token != null) {
        await this.logout({
          ignoreError: true,
          sync: true,
          callHttpLogout: false,
          returnTo: this.resolveCurrentProtectedReturnTo(),
        });
      }
      throw err;
    }
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

  async submitLogin(body: LoginDto): Promise<LoginResultDto> {
    try {
      this.userService.changeLoading(true);
      const response = await this.authHttpService.login(body);
      const mfaRequired = response.body?.mfaRequired ?? false;

      if (!mfaRequired) {
        await this.loginSuccess(response);
      } else {
        this.userService.changeLoading(false);
      }

      return response.body!!;
    } catch (err) {
      this.userService.changeLoading(false);
      throw err;
    }
  }

  async submitMfa(body: LoginMfaDto): Promise<void> {
    try {
      this.userService.changeLoading(true);
      const response = await this.authHttpService.mfa(body);
      await this.loginSuccess(response);
    } catch (err) {
      this.userService.changeLoading(false);
      throw err;
    }
  }

  async submitRegistration(body: RegisterDto): Promise<RegisterResultDto> {
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
    }

    if (token != null && args.sync) {
      this.tokenSyncService.postLogoutMessage();
    }

    this.tokenStateService.changeToken(null);
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

  async refreshSessionFromUnauthorized(): Promise<string | null> {
    try {
      return await this.refreshJwt();
    } catch {
      return null;
    }
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
        returnTo: this.resolveCurrentProtectedReturnTo(),
      });
      throw err;
    }

    return await lastValueFrom(this.tokenStateService.token$.pipe(take(1)));
  }

  private async loginSuccess(response: HttpResponse<any>) {
    const token = this.getTokenFromHeaders(response.headers);
    await this.changeTokenAndSync(token);
  }

  private getTokenFromHeaders(headers: HttpHeaders): string | null {
    const value = headers.get('Authorization');

    if (value) return value.replace('Bearer ', '');
    return null;
  }

  private navigateAfterLoginSuccess() {
    const returnTo = this.resolveReturnToFromQuery();
    return this.router.navigateByUrl(returnTo ?? '/app');
  }

  private async navigateAfterLogoutSuccess(return_to: string | undefined) {
    await this.router.navigate(['/login'], {
      onSameUrlNavigation: 'reload',
      queryParams: {
        return_to,
      },
    });
  }

  private resolveCurrentProtectedReturnTo(): string | undefined {
    const url = this.router.url?.length > 0 ? this.router.url : `${window.location.pathname}${window.location.search}`;

    if (url.startsWith('/app') || url.startsWith('/welcome')) {
      return url;
    }

    return undefined;
  }

  private resolveReturnToFromQuery(): string | undefined {
    const rawReturnTo = this.activeRoute.snapshot.queryParamMap.get('return_to');

    if (rawReturnTo == null || rawReturnTo.trim().length === 0) {
      return undefined;
    }

    if (!rawReturnTo.startsWith('/')) {
      return undefined;
    }

    if (rawReturnTo.startsWith('/login') || rawReturnTo.startsWith('/register')) {
      return undefined;
    }

    return rawReturnTo;
  }
}
