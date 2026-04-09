import { HttpErrorResponse, HttpHandlerFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { BehaviorSubject, lastValueFrom, of, throwError } from 'rxjs';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../services/auth.service';
import { TokenStateService } from '../services/token-state.service';
import { apiAuthInterceptor } from './unauthorized.interceptor';

describe('apiAuthInterceptor', () => {
  const tokenStateServiceMock = {
    token$: new BehaviorSubject<string | null>('initial-token'),
  };

  const authServiceMock = {
    refreshSessionFromUnauthorized: vi.fn<() => Promise<string | null>>(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    tokenStateServiceMock.token$ = new BehaviorSubject<string | null>('initial-token');

    await TestBed.configureTestingModule({
      providers: [
        { provide: TokenStateService, useValue: tokenStateServiceMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();
  });

  it('retries once with refreshed token after 401', async () => {
    authServiceMock.refreshSessionFromUnauthorized.mockResolvedValue('new-token');
    tokenStateServiceMock.token$.next('new-token');

    const next = vi.fn((req: HttpRequest<unknown>) =>
      of(new HttpResponse({ status: 200, body: { ok: true, authorization: req.headers.get('Authorization') } })),
    );
    next.mockImplementationOnce((req: HttpRequest<unknown>) =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            url: req.url,
          }),
      ),
    );

    const request = new HttpRequest('GET', '/api/wallet-entries');

    const result = await TestBed.runInInjectionContext(async () => {
      return await lastValueFrom(apiAuthInterceptor(request, next as unknown as HttpHandlerFn));
    });

    expect(authServiceMock.refreshSessionFromUnauthorized).toHaveBeenCalledTimes(1);
    expect((next as any).mock.calls).toHaveLength(2);
    expect((next as any).mock.calls[1][0].headers.get('Authorization')).toBe('Bearer new-token');
    expect((result as HttpResponse<any>).status).toBe(200);
  });

  it('propagates original 401 after refresh failure', async () => {
    authServiceMock.refreshSessionFromUnauthorized.mockResolvedValue(null);

    const next = vi.fn((req: HttpRequest<unknown>) =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            url: req.url,
          }),
      ),
    );

    const request = new HttpRequest('GET', '/api/wallet-entries');

    await expect(
      TestBed.runInInjectionContext(async () => {
        return await lastValueFrom(apiAuthInterceptor(request, next as unknown as HttpHandlerFn));
      }),
    ).rejects.toMatchObject({ status: 401 });

    expect(authServiceMock.refreshSessionFromUnauthorized).toHaveBeenCalledTimes(1);
    expect((next as any).mock.calls).toHaveLength(1);
  });

  it('does not attempt refresh for open auth endpoint', async () => {
    const next = vi.fn((req: HttpRequest<unknown>) =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            url: req.url,
          }),
      ),
    );

    const request = new HttpRequest('POST', '/api/open/auth/refresh', null);

    await expect(
      TestBed.runInInjectionContext(async () => {
        return await lastValueFrom(apiAuthInterceptor(request, next as unknown as HttpHandlerFn));
      }),
    ).rejects.toMatchObject({ status: 401 });

    expect(authServiceMock.refreshSessionFromUnauthorized).not.toHaveBeenCalled();
    expect((next as any).mock.calls).toHaveLength(1);
  });

  it('does not refresh more than once when retry still returns 401', async () => {
    authServiceMock.refreshSessionFromUnauthorized.mockResolvedValue('new-token');
    tokenStateServiceMock.token$.next('new-token');

    const next = vi.fn((req: HttpRequest<unknown>) =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            statusText: 'Unauthorized',
            url: req.url,
          }),
      ),
    );

    const request = new HttpRequest('GET', '/api/wallet-entries');

    await expect(
      TestBed.runInInjectionContext(async () => {
        return await lastValueFrom(apiAuthInterceptor(request, next as unknown as HttpHandlerFn));
      }),
    ).rejects.toMatchObject({ status: 401 });

    expect(authServiceMock.refreshSessionFromUnauthorized).toHaveBeenCalledTimes(1);
    expect((next as any).mock.calls).toHaveLength(2);
  });
});
