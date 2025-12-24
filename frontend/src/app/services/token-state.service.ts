import { Injectable } from '@angular/core';

import { BehaviorSubject, filter } from 'rxjs';

const TOKEN_STORAGE_KEY = 'token';

@Injectable({ providedIn: 'root' })
export class TokenStateService {
  private readonly tokenSubject = new BehaviorSubject<string | null | undefined>(localStorage.getItem(TOKEN_STORAGE_KEY));
  readonly token$ = this.tokenSubject.pipe(filter(t => t !== undefined));

  changeToken(token: string | null | undefined) {
    if (this.tokenSubject.value === token) return;

    if (token) {
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }

    this.tokenSubject.next(token);
  }
}
