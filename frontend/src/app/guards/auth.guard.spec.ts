import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import { UserService } from '../services/user.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let router: Router;
  const userServiceMock = {
    getUser: vi.fn<() => Promise<any>>(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: UserService, useValue: userServiceMock }],
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('returns login UrlTree with return_to when user is null', async () => {
    userServiceMock.getUser.mockResolvedValue(null);

    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, { url: '/app/transactions/new?x=1' } as any));

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?return_to=%2Fapp%2Ftransactions%2Fnew%3Fx%3D1');
  });

  it('returns true when user exists', async () => {
    userServiceMock.getUser.mockResolvedValue({ id: 'user-1' });

    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, { url: '/app' } as any));

    expect(result).toBe(true);
  });

  it('returns login UrlTree when user loading throws', async () => {
    userServiceMock.getUser.mockRejectedValue(new Error('expired'));

    const result = await TestBed.runInInjectionContext(() => authGuard({} as any, { url: '/app/bankAccounts?foo=bar' } as any));

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login?return_to=%2Fapp%2FbankAccounts%3Ffoo%3Dbar');
  });
});
