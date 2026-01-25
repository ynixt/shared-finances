import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { LoginDto, LoginMfaDto, LoginResultDto, RegisterDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/auth';
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

  async register(body: RegisterDto): Promise<object> {
    return lastValueFrom(this.httpClient.post('/api/open/auth/register', body).pipe(take(1)));
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
