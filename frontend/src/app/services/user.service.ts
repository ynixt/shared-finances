import { HttpClient } from '@angular/common/http';
import { Injectable, effect, signal } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { User } from '../models/user';
import { KratosAuthService } from './kratos-auth.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly _user = signal<User | null>(null);
  private readonly _loading = signal(false);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();

  constructor(
    private http: HttpClient,
    private auth: KratosAuthService,
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

  getUserFromHttp(): Promise<User> {
    return lastValueFrom(this.http.get<User>('/api/users/current').pipe(take(1)));
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
}
