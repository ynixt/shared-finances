import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, WritableSignal, inject, signal } from '@angular/core';

import { toObservable } from '@angular/core/rxjs-interop';
import { combineLatest, filter, firstValueFrom, lastValueFrom, map, take } from 'rxjs';

import { ChangePasswordDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
import {
  ConfirmMfaRequestDto,
  ConfirmMfaResponseDto, DisableMfaRequestDto,
  EnableMfaRequestDto,
  EnableMfaResponseDto,
} from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth/mfa';
import { UpdateUserDto, UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { UserMissingError } from '../pages/finances/errors/user-missing.error';

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

  async changePassword(changePasswordRequest: ChangePasswordDto): Promise<void> {
    const currentUser = this.user();

    if (currentUser == null) return;

    await lastValueFrom(this.http.put(`/api/users/current/changePassword`, changePasswordRequest).pipe(take(1)));
  }

  async updateCurrentUser(request: UpdateUserDto, avatar: File | undefined): Promise<UserResponseDto> {
    const currentUser = this.user();

    if (currentUser == null) new UserMissingError();

    const form = new FormData();
    form.append('dto', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    if (avatar) {
      form.append('avatar', avatar, avatar.name);
    }

    const newUser = await lastValueFrom(this.http.put<UserResponseDto>(`/api/users/current`, form).pipe(take(1)));

    this.changeUser(newUser);

    return newUser;
  }

  async enableMfaBegin(request: EnableMfaRequestDto): Promise<EnableMfaResponseDto> {
    const currentUser = this.user();

    if (currentUser == null) new UserMissingError();

    return await lastValueFrom(this.http.post<EnableMfaResponseDto>(`/api/mfa-settings/begin`, request).pipe(take(1)));
  }

  async enableMfaConfirm(request: ConfirmMfaRequestDto): Promise<ConfirmMfaResponseDto> {
    const currentUser = this.user();

    if (currentUser == null) new UserMissingError();

    return await lastValueFrom(this.http.post<ConfirmMfaResponseDto>(`/api/mfa-settings/confirm`, request).pipe(take(1)));
  }

  async disableMfa(request: DisableMfaRequestDto): Promise<void> {
    const currentUser = this.user();

    if (currentUser == null) new UserMissingError();

    await lastValueFrom(this.http.post(`/api/mfa-settings/disable`, request).pipe(take(1)));
  }
}
