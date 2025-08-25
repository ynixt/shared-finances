import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, WritableSignal, effect, signal } from '@angular/core';

import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, filter, firstValueFrom, lastValueFrom, map, take } from 'rxjs';

import { User } from '../models/user';
import { DEFAULT_ERROR_LIFE } from '../util/error-util';
import { KratosAuthService } from './kratos-auth.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly _user = signal<User | null>(null);
  private readonly _loading = signal(true);
  private readonly _error: WritableSignal<any> = signal(null);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

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
        this._error.set(null);
        return;
      }

      this._error.set(null);
      this._loading.set(true);
      this.getUserFromHttp()
        .then(u => {
          this._user.set(u);
        })
        .catch(err => {
          this._user.set(null);
          this._error.set(err);
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
