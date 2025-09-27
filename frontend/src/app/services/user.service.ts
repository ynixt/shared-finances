import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, WritableSignal, signal } from '@angular/core';

import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, filter, firstValueFrom, lastValueFrom, map, take } from 'rxjs';

import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { KratosAuthService } from './kratos-auth.service';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly _user = signal<UserResponseDto | null>(null);
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
    this.auth.token$.subscribe(token => {
      const currentUser = this.user();

      if (token == null) {
        this._user.set(null);
        this._loading.set(false);
        this._error.set(null);
        return;
      }

      if (currentUser) {
        this._error.set(null);
        this._loading.set(false);
      } else {
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
      }
    });
  }

  async getUser(): Promise<UserResponseDto | null> {
    await lastValueFrom(this.auth.token$.pipe(take(1)));

    return firstValueFrom(
      combineLatest([toObservable(this.user, { injector: this.injector }), toObservable(this.loading, { injector: this.injector })]).pipe(
        filter(([user, loading]) => loading === false),
        map(([user]) => user),
        take(1),
      ),
    );
  }

  changeUser(user: UserResponseDto) {
    this._user.set(user);
    this._loading.set(false);
    this._error.set(null);
  }

  async changeDefaultCurrency(newDefaultCurrency: string): Promise<void> {
    const currentUser = this.user();

    if (currentUser == null) return;

    await lastValueFrom(this.http.put(`/api/users/current/changeDefaultCurrency/${newDefaultCurrency}`, null).pipe(take(1)));

    this._user.set({
      ...currentUser,
      defaultCurrency: newDefaultCurrency,
    });
  }

  private getUserFromHttp(): Promise<UserResponseDto> {
    return lastValueFrom(this.http.get<UserResponseDto>('/api/users/current').pipe(take(1)));
  }
}
