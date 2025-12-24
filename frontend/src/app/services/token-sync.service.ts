import { Injectable } from '@angular/core';

import { Subject } from 'rxjs';

import { BroadcastChannel } from 'broadcast-channel';

import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { generateUuidv4 } from '../util/uuid';

interface LoginMessage {
  type: 'login';
  user: UserResponseDto;
  token: string;
  tabId: string;
}

interface LogoutMessage {
  type: 'logout';
  tabId: string;
}

interface TokenUpdatedMessage {
  type: 'token-updated';
  token: string;
  tabId: string;
}

const tabId = generateUuidv4();
console.log(tabId);

@Injectable({ providedIn: 'root' })
export class TokenSyncService {
  private loginMessageSubject = new Subject<LoginMessage>();
  private logoutMessageSubject = new Subject<LogoutMessage>();
  private newTokenMessageSubject = new Subject<TokenUpdatedMessage>();

  private channel = new BroadcastChannel<LoginMessage | LogoutMessage | TokenUpdatedMessage>('auth');
  // private leader = createLeaderElection(this.channel);

  public onLoginMessage = this.loginMessageSubject.asObservable();
  public onLogoutMessage = this.logoutMessageSubject.asObservable();
  public newTokenMessage = this.newTokenMessageSubject.asObservable();

  constructor() {
    this.channel.onmessage = msg => {
      if (msg == null || msg.tabId === tabId) return;

      if (msg.type === 'login') {
        this.loginMessageSubject.next(msg);
      } else if (msg.type === 'logout') {
        this.logoutMessageSubject.next(msg);
      } else if (msg.type === 'token-updated') {
        this.newTokenMessageSubject.next(msg);
      }
    };
  }

  // async isLeader(): Promise<boolean> {
  //   try {
  //     await this.withTimeout(this.leader.awaitLeadership(), 500);
  //     return true;
  //   } catch (error) {
  //     if (error instanceof PromiseTimeoutError) {
  //       return false;
  //     }
  //     throw error;
  //   }
  // }

  async newLogin(user: UserResponseDto, token: string) {
    return this.postLoginMessage(token, user);
  }

  private postLoginMessage(token: string, user: UserResponseDto) {
    return this.channel.postMessage({ type: 'login', user, token, tabId });
  }

  postLogoutMessage() {
    return this.channel.postMessage({ type: 'logout', tabId });
  }

  postTokenUpdatedMessage(token: string) {
    return this.channel.postMessage({ type: 'token-updated', token, tabId });
  }

  // private withTimeout<T>(p: Promise<T>, ms: number, message = 'Timeout'): Promise<T> {
  //   return new Promise<T>((resolve, reject) => {
  //     const id = setTimeout(() => reject(new PromiseTimeoutError(message)), ms);
  //     p.then(
  //       v => {
  //         clearTimeout(id);
  //         resolve(v);
  //       },
  //       e => {
  //         clearTimeout(id);
  //         reject(e);
  //       },
  //     );
  //   });
  // }
}

class PromiseTimeoutError extends Error {
  constructor(message = 'Timeout') {
    super(message);
    this.name = 'TimeoutError';
  }
}
