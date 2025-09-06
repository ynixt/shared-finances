import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { filter } from 'rxjs';

import { BroadcastChannel, createLeaderElection } from 'broadcast-channel';

import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto';
import { KratosAuthService } from './kratos-auth.service';
import { UserService } from './user.service';

interface LoginMessage {
  type: 'login';
  user: UserResponseDto;
  token: string;
}

interface LogoutMessage {
  type: 'logout';
}

interface TokenUpdatedMessage {
  type: 'token-updated';
  token: string;
}

@Injectable({ providedIn: 'root' })
export class TokenSyncService {
  private channel = new BroadcastChannel<LoginMessage | LogoutMessage | TokenUpdatedMessage>('auth');
  private leader = createLeaderElection(this.channel);

  private logoutReceived = false;

  constructor(
    private authService: KratosAuthService,
    private userService: UserService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {
    this.channel.onmessage = msg => {
      if (msg == null) return;

      if (msg.type === 'login') {
        this.userService.changeUser(msg.user);
        this.authService.tokenSubject.next(msg.token);
        this.authService.loginSuccess();
      } else if (msg.type === 'logout') {
        this.logoutReceived = true;
        this.tokenInvalid(false);
      } else if (msg.type === 'token-updated') {
        console.log('new token received');
        this.authService.tokenSubject.next(msg.token);
      }
    };

    this.authService.token$.pipe(filter(token => token == null)).subscribe(() => {
      if (!this.logoutReceived) {
        this.postLogoutMessage();
      }

      this.logoutReceived = false;
    });
  }

  async refreshOnce(): Promise<void> {
    try {
      await this.withTimeout(this.leader.awaitLeadership(), 500);
    } catch (error) {
      if (error instanceof PromiseTimeoutError) {
        return;
      }
      throw error;
    }

    console.log('refreshing token');

    try {
      const token = await this.authService.refreshJwt();

      if (token != null) {
        this.postTokenUpdatedMessage(token);
      } else {
        this.tokenInvalid(true);
      }
    } catch (error) {
      console.error(error);

      this.tokenInvalid(true);
    }
  }

  async newLogin(user: UserResponseDto, token: string) {
    return this.postLoginMessage(token, user);
  }

  private postLoginMessage(token: string, user: UserResponseDto) {
    return this.channel.postMessage({ type: 'login', user, token });
  }

  private async tokenInvalid(postLogoutMessage: boolean) {
    let returnTo = this.activatedRoute.snapshot.queryParamMap.get('return_to');

    if (returnTo == null && this.router.url != '/login' && this.router.url != '/app') {
      returnTo = this.router.url;
    }

    await this.authService.logout({
      alsoLogoutKratos: false,
      returnTo: returnTo == null ? undefined : returnTo,
    });

    if (postLogoutMessage) {
      this.postLogoutMessage();
    }
  }

  private postLogoutMessage() {
    return this.channel.postMessage({ type: 'logout' });
  }

  private postTokenUpdatedMessage(token: string) {
    return this.channel.postMessage({ type: 'token-updated', token });
  }

  private withTimeout<T>(p: Promise<T>, ms: number, message = 'Timeout'): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      const id = setTimeout(() => reject(new PromiseTimeoutError(message)), ms);
      p.then(
        v => {
          clearTimeout(id);
          resolve(v);
        },
        e => {
          clearTimeout(id);
          reject(e);
        },
      );
    });
  }
}

class PromiseTimeoutError extends Error {
  constructor(message = 'Timeout') {
    super(message);
    this.name = 'TimeoutError';
  }
}
