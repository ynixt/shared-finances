import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BehaviorSubject, Subject } from 'rxjs';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { AuthHttpService } from './auth-http.service';
import { AuthService } from './auth.service';
import { GuardInspector } from './guard-inspector.service';
import { TokenStateService } from './token-state.service';
import { TokenSyncService } from './token-sync.service';
import { UserService } from './user.service';

function buildUser(): UserResponseDto {
  return {
    id: 'user-1',
    firstName: 'User',
    lastName: 'One',
    email: 'user@site.com',
    defaultCurrency: 'BRL',
    emailVerified: true,
    lang: 'pt-BR',
    mfaEnabled: false,
    onboardingDone: true,
    tmz: 'America/Sao_Paulo',
    photoUrl: null,
  };
}

describe('AuthService', () => {
  let tokenSubject: BehaviorSubject<string | null>;
  let loginMessages: Subject<any>;
  let logoutMessages: Subject<any>;
  let tokenUpdatedMessages: Subject<any>;

  let routeReturnTo: string | null;
  let activatedRouteReturnTo: string | null;

  const routerMock = {
    url: '/app',
    navigate: vi.fn<(...args: any[]) => Promise<boolean>>().mockResolvedValue(true),
    navigateByUrl: vi.fn<(url: string) => Promise<boolean>>().mockResolvedValue(true),
  };

  const guardInspectorMock = {
    hasCanActivateInHierarchy: vi.fn<(guard: any) => boolean>().mockReturnValue(true),
  };

  const authHttpServiceMock = {
    getUser: vi.fn<() => Promise<UserResponseDto>>(),
    refreshJwt: vi.fn<() => Promise<HttpResponse<Object>>>(),
    logout: vi.fn<() => Promise<void>>().mockResolvedValue(undefined),
    login: vi.fn(),
    mfa: vi.fn(),
    register: vi.fn(),
  };

  const tokenStateServiceMock = {
    token$: undefined as any,
    changeToken: vi.fn<(token: string | null | undefined) => void>(),
  };

  const tokenSyncServiceMock = {
    onLoginMessage: undefined as any,
    onLogoutMessage: undefined as any,
    newTokenMessage: undefined as any,
    newLogin: vi.fn(),
    postLogoutMessage: vi.fn(),
    postTokenUpdatedMessage: vi.fn(),
  };

  const userSignal = signal<UserResponseDto | null>(null);
  const userLoadingSignal = signal(true);
  const userErrorSignal = signal<any>(null);

  const userServiceMock = {
    user: userSignal,
    loading: userLoadingSignal,
    error: userErrorSignal,
    getUser: vi.fn<() => Promise<UserResponseDto | null>>(async () => userSignal()),
    changeUser: vi.fn<(user: UserResponseDto | null, err?: any) => void>(),
  };

  beforeEach(() => {
    vi.useFakeTimers();
    vi.clearAllMocks();

    routeReturnTo = null;
    activatedRouteReturnTo = null;

    routerMock.url = '/app';

    tokenSubject = new BehaviorSubject<string | null>(null);
    tokenStateServiceMock.token$ = tokenSubject.asObservable();
    tokenStateServiceMock.changeToken.mockImplementation(token => {
      tokenSubject.next((token ?? null) as string | null);
    });

    loginMessages = new Subject<any>();
    logoutMessages = new Subject<any>();
    tokenUpdatedMessages = new Subject<any>();
    tokenSyncServiceMock.onLoginMessage = loginMessages.asObservable();
    tokenSyncServiceMock.onLogoutMessage = logoutMessages.asObservable();
    tokenSyncServiceMock.newTokenMessage = tokenUpdatedMessages.asObservable();

    userSignal.set(null);
    userLoadingSignal.set(true);
    userErrorSignal.set(null);
    userServiceMock.changeUser.mockImplementation((user, err) => {
      userSignal.set(user);
      userLoadingSignal.set(false);
      userErrorSignal.set(err ?? null);
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function buildService() {
    const activeRouteMock = {
      snapshot: {
        queryParamMap: {
          get: (key: string) => (key === 'return_to' ? routeReturnTo : null),
        },
      },
    } as unknown as ActivatedRoute;

    const activatedRouteMock = {
      snapshot: {
        queryParamMap: {
          get: (key: string) => (key === 'return_to' ? activatedRouteReturnTo : null),
        },
      },
    } as unknown as ActivatedRoute;

    return new AuthService(
      routerMock as any,
      activeRouteMock,
      guardInspectorMock as unknown as GuardInspector,
      userServiceMock as unknown as UserService,
      tokenSyncServiceMock as unknown as TokenSyncService,
      activatedRouteMock,
      authHttpServiceMock as unknown as AuthHttpService,
      tokenStateServiceMock as unknown as TokenStateService,
    );
  }

  it('finalizes bootstrap as unauthenticated when startup token is missing', async () => {
    buildService();

    expect(authHttpServiceMock.getUser).not.toHaveBeenCalled();
    expect(userServiceMock.changeUser).toHaveBeenCalledWith(null);
    expect(userSignal()).toBeNull();
    expect(userLoadingSignal()).toBe(false);
  });

  it('forces logout with return_to when refresh fails while loading user', async () => {
    const service = buildService();
    userServiceMock.changeUser.mockClear();

    tokenStateServiceMock.changeToken('expired-token');
    routerMock.url = '/app/transactions/scheduler-manager/edit/rec-1?withFuture=true';
    authHttpServiceMock.getUser.mockRejectedValue(
      new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
      }),
    );
    authHttpServiceMock.refreshJwt.mockRejectedValue(
      new HttpErrorResponse({
        status: 401,
        statusText: 'Unauthorized',
      }),
    );

    await expect(service.loadUser()).rejects.toMatchObject({ status: 401 });
    await vi.runAllTimersAsync();

    expect(tokenSyncServiceMock.postLogoutMessage).toHaveBeenCalledTimes(1);
    expect(tokenSubject.value).toBeNull();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login'], {
      onSameUrlNavigation: 'reload',
      queryParams: {
        return_to: '/app/transactions/scheduler-manager/edit/rec-1?withFuture=true',
      },
    });
    expect(userServiceMock.changeUser).toHaveBeenCalledWith(
      null,
      expect.objectContaining({
        status: 401,
      }),
    );
  });

  it('keeps cross-tab login and token sync behavior', async () => {
    buildService();
    const user = buildUser();
    userServiceMock.changeUser.mockClear();
    routeReturnTo = '/app/transactions/new?withFuture=true';

    loginMessages.next({
      type: 'login',
      user,
      token: 'token-tab-a',
      tabId: 'other-tab',
    });

    await vi.runAllTimersAsync();

    expect(userServiceMock.changeUser).toHaveBeenCalledWith(user);
    expect(tokenSubject.value).toBe('token-tab-a');
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/app/transactions/new?withFuture=true');

    tokenUpdatedMessages.next({
      type: 'token-updated',
      token: 'token-rotated',
      tabId: 'other-tab',
    });

    expect(tokenSubject.value).toBe('token-rotated');

    routeReturnTo = '/login?invalid=true';
    loginMessages.next({
      type: 'login',
      user,
      token: 'token-tab-a',
      tabId: 'other-tab',
    });
    await vi.runAllTimersAsync();

    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/app');
  });

  it('handles logout message without rebroadcasting logout event', async () => {
    buildService();
    tokenStateServiceMock.changeToken('current-token');
    routerMock.url = '/app/creditCards?month=2026-04';
    activatedRouteReturnTo = null;

    logoutMessages.next({
      type: 'logout',
      tabId: 'other-tab',
    });

    await vi.runAllTimersAsync();

    expect(tokenSubject.value).toBeNull();
    expect(tokenSyncServiceMock.postLogoutMessage).not.toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login'], {
      onSameUrlNavigation: 'reload',
      queryParams: {
        return_to: '/app/creditCards?month=2026-04',
      },
    });
  });

  it('returns refreshed token and broadcasts token update', async () => {
    const service = buildService();
    tokenStateServiceMock.changeToken('token-old');
    const refreshedUser = buildUser();
    authHttpServiceMock.refreshJwt.mockResolvedValue(
      new HttpResponse({
        status: 200,
        headers: new HttpHeaders({
          Authorization: 'Bearer token-new',
        }),
      }),
    );
    authHttpServiceMock.getUser.mockResolvedValue(refreshedUser);
    userServiceMock.changeUser.mockClear();

    const result = await service.refreshSessionFromUnauthorized();

    expect(result).toBe('token-new');
    expect(tokenSubject.value).toBe('token-new');
    expect(tokenSyncServiceMock.postTokenUpdatedMessage).toHaveBeenCalledWith('token-new');
    expect(userServiceMock.changeUser).toHaveBeenCalledWith(refreshedUser);
  });
});
