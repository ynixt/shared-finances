import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  ChangePendingEmailRequestDto,
  ConfirmEmailRequestDto,
  EmailTurnstileRequestDto,
  GenericAckDto,
  LoginDto,
  LoginMfaDto,
  LoginResultDto,
  PasswordResetConfirmRequestDto,
  RegisterDto,
  RegisterResultDto,
  ResendEmailAckDto,
} from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';

@Injectable({ providedIn: 'root' })
export class AuthHttpService {
  constructor(private httpClient: HttpClient) {}

  getUser(): Promise<UserResponseDto> {
    return lastValueFrom(this.httpClient.get<UserResponseDto>('/api/users/current').pipe(take(1)));
  }

  login(body: LoginDto): Promise<HttpResponse<LoginResultDto>> {
    return lastValueFrom(
      this.httpClient
        .post<LoginResultDto>('/api/open/auth/login', body, {
          observe: 'response',
        })
        .pipe(take(1)),
    );
  }

  mfa(body: LoginMfaDto): Promise<HttpResponse<object>> {
    return lastValueFrom(
      this.httpClient
        .post('/api/open/auth/mfa', body, {
          observe: 'response',
        })
        .pipe(take(1)),
    );
  }

  register(body: RegisterDto): Promise<RegisterResultDto> {
    return lastValueFrom(this.httpClient.post<RegisterResultDto>('/api/open/auth/register', body).pipe(take(1)));
  }

  confirmEmail(body: ConfirmEmailRequestDto): Promise<void> {
    return lastValueFrom(this.httpClient.post<void>('/api/open/auth/confirm-email', body).pipe(take(1)));
  }

  resendConfirmationEmail(body: EmailTurnstileRequestDto): Promise<ResendEmailAckDto> {
    return lastValueFrom(this.httpClient.post<ResendEmailAckDto>('/api/open/auth/resend-confirmation-email', body).pipe(take(1)));
  }

  changePendingEmail(body: ChangePendingEmailRequestDto): Promise<ResendEmailAckDto> {
    return lastValueFrom(this.httpClient.post<ResendEmailAckDto>('/api/open/auth/change-pending-email', body).pipe(take(1)));
  }

  forgotPassword(body: EmailTurnstileRequestDto): Promise<GenericAckDto> {
    return lastValueFrom(this.httpClient.post<GenericAckDto>('/api/open/auth/forgot-password', body).pipe(take(1)));
  }

  resendForgotPassword(body: EmailTurnstileRequestDto): Promise<ResendEmailAckDto> {
    return lastValueFrom(this.httpClient.post<ResendEmailAckDto>('/api/open/auth/resend-forgot-password', body).pipe(take(1)));
  }

  resetPassword(body: PasswordResetConfirmRequestDto): Promise<void> {
    return lastValueFrom(this.httpClient.post<void>('/api/open/auth/reset-password', body).pipe(take(1)));
  }

  async logout(): Promise<void> {
    await lastValueFrom(this.httpClient.post('/api/auth/logout', undefined).pipe(take(1)));
  }

  refreshJwt(): Promise<HttpResponse<Object>> {
    return lastValueFrom(
      this.httpClient
        .post('/api/open/auth/refresh', undefined, {
          observe: 'response',
        })
        .pipe(take(1)),
    );
  }
}
