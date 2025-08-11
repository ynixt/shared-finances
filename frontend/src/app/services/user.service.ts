import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, effect, signal } from '@angular/core';

import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, filter, firstValueFrom, lastValueFrom, map, take } from 'rxjs';

import { User } from '../models/user';
import { KratosAuthService } from './kratos-auth.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly _user = signal<User | null>(null);
  private readonly _loading = signal(true);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();

  constructor(
    private http: HttpClient,
    private auth: KratosAuthService,
    private injector: Injector,
  ) {
    effect(() => {
      const token = this.auth.token();

      if (token == null) {
        this._user.set(null);
        this._loading.set(false);
        return;
      }

      this._loading.set(true);
      this.getUserFromHttp()
        .then(u => this._user.set(u))
        .catch(() => {
          this._user.set(null);
          this.auth.logout();
        })
        .finally(() => this._loading.set(false));
    });
  }

  async getUser(): Promise<User | null> {
    if (this.auth.token() == null) {
      await this.auth.getToken();
    }

    return firstValueFrom(
      combineLatest([toObservable(this.user, { injector: this.injector }), toObservable(this.loading, { injector: this.injector })]).pipe(
        filter(([user, loading]) => loading === false),
        map(([user]) => user),
        take(1),
      ),
    );
  }

  async refreshUser(): Promise<void> {
    const token = this.auth.token();
    if (token == null) {
      this._user.set(null);
      return;
    }
    this._loading.set(true);
    try {
      const u = await this.getUserFromHttp();
      this._user.set(u);
    } catch {
      this._user.set(null);
    } finally {
      this._loading.set(false);
    }
  }

  private getUserFromHttp(): Promise<User> {
    return lastValueFrom(this.http.get<User>('/api/users/current').pipe(take(1)));
  }
}
