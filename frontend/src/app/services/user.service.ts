import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, Signal, computed, inject, signal } from '@angular/core';

import { toObservable } from '@angular/core/rxjs-interop';
import { filter, lastValueFrom, map, take } from 'rxjs';

import { ChangePasswordDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
import {
  ConfirmMfaRequestDto,
  ConfirmMfaResponseDto,
  DisableMfaRequestDto,
  EnableMfaRequestDto,
  EnableMfaResponseDto,
} from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth/mfa';
import { UpdateUserDto, UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { UserMissingError } from '../pages/finances/errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly injector = inject(Injector);

  private readonly state = signal<{
    user: UserResponseDto | null;
    loading: boolean;
    error: any;
  }>({
    user: null,
    loading: true,
    error: null,
  });

  readonly error: Signal<any> = computed(() => this.state().error);
  readonly loading = computed(() => this.state().loading);
  readonly user = computed(() => this.state().user);

  readonly user$ = toObservable(this.state).pipe(map(state => (state.loading ? undefined : state.user)));

  private readonly userWhenReady$ = this.user$.pipe(
    filter(user => user !== undefined),
    map(user => user),
  );

  async getUser(): Promise<UserResponseDto | null> {
    return lastValueFrom(this.userWhenReady$.pipe(take(1)));
  }

  changeUser(user: UserResponseDto | null, error?: any) {
    this.state.set({
      user,
      loading: false,
      error,
    });
  }

  changeLoading(newLoading: boolean) {
    const currentState = this.state();

    this.state.set({
      user: currentState.user,
      loading: newLoading,
      error: currentState.error,
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

  async deleteCurrentAccount(): Promise<void> {
    const currentUser = this.user();

    if (currentUser == null) throw new UserMissingError();

    await lastValueFrom(this.http.delete(`/api/users/current`).pipe(take(1)));
  }
}
