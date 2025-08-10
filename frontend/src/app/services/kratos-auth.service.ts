import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { BehaviorSubject, Observable, firstValueFrom, lastValueFrom, take, tap } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KratosAuthService {
  private readonly kratos = environment.kratosPublicUrl;
  readonly token$ = new BehaviorSubject<string | null>(null);

  constructor(private http: HttpClient) {}

  // ---------- FLOW ----------
  async getLoginFlow(returnTo = '/'): Promise<any> {
    return lastValueFrom(
      this.http.get(`${this.kratos}/self-service/login/browser?return_to=${returnTo}`, { withCredentials: true }).pipe(take(1)),
    );
  }

  async getRegistrationFlow(returnTo = '/login'): Promise<any> {
    return lastValueFrom(
      this.http.get(`${this.kratos}/self-service/registration/browser?return_to=${returnTo}`, { withCredentials: true }).pipe(take(1)),
    );
  }

  // ---------- SUBMIT FORMS ----------
  submitLoginFlow(flowId: string, body: any): Promise<any> {
    return lastValueFrom(this.http.post(`${this.kratos}/self-service/login?flow=${flowId}`, body, { withCredentials: true }).pipe(take(1)));
  }

  submitRegistrationFlow(flowId: string, body: any): Promise<object> {
    return lastValueFrom(
      this.http.post(`${this.kratos}/self-service/registration?flow=${flowId}`, body, { withCredentials: true }).pipe(take(1)),
    );
  }

  // ---------- LOGOUT ----------
  logout(): void {
    window.location.href = `${this.kratos}/self-service/logout/browser`;
  }

  // ---------- SESSÃO / WHOAMI ----------
  /**
   * Retorna os dados da sessão atual ou lança 401/403 se não autenticado.
   */
  getSession(): Promise<any> {
    return lastValueFrom(this.http.get<any>(`${this.kratos}/sessions/whoami`, { withCredentials: true }).pipe(take(1)));
  }

  async refreshJwt(): Promise<void> {
    const params = new HttpParams().set('tokenize_as', 'default_jwt');

    await lastValueFrom(
      this.http
        .get<{ tokenized: string }>(
          `${this.kratos}/sessions/whoami`,
          { params, withCredentials: true }, // envia o cookie HttpOnly
        )
        .pipe(
          tap({
            next: ({ tokenized }) => this.token$.next(tokenized),
            error: () => this.token$.next(null),
          }),
          take(1),
        ),
    );
  }

  async getToken(): Promise<string | null> {
    const token = await lastValueFrom(this.token$.pipe(take(1)));

    if (token != null) {
      return token;
    }

    await this.refreshJwt();
    return this.getToken();
  }
}
