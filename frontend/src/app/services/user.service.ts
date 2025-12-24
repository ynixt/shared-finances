import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, WritableSignal, inject, signal } from '@angular/core';

import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, filter, firstValueFrom, lastValueFrom, map, take } from 'rxjs';

import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';

@Injectable({ providedIn: 'root' })
export class UserService {
  readonly user = signal<UserResponseDto | null>(null);
  readonly loading = signal(true);
  readonly error: WritableSignal<any> = signal(null);
  private readonly http = inject(HttpClient);
  private readonly injector = inject(Injector);

  private readonly user$ = toObservable(this.user, { injector: this.injector });
  private readonly loading$ = toObservable(this.loading, { injector: this.injector });

  private readonly userWhenReady$ = combineLatest([this.user$, this.loading$]).pipe(
    filter(([_, loading]) => loading === false),
    map(([user]) => user),
  );

  async getUser(): Promise<UserResponseDto | null> {
    return firstValueFrom(this.userWhenReady$.pipe(take(1)));
  }

  changeUser(user: UserResponseDto | null, err?: any) {
    this.user.set(user);
    this.loading.set(false);
    this.error.set(err);
  }

  async changeDefaultCurrency(newDefaultCurrency: string): Promise<void> {
    const currentUser = this.user();

    if (currentUser == null) return;

    await lastValueFrom(this.http.put(`/api/users/current/changeDefaultCurrency/${newDefaultCurrency}`, null).pipe(take(1)));

    this.user.set({
      ...currentUser,
      defaultCurrency: newDefaultCurrency,
    });
  }

  getUserFromHttp(): Promise<UserResponseDto> {
    return lastValueFrom(this.http.get<UserResponseDto>('/api/users/current').pipe(take(1)));
  }
}
