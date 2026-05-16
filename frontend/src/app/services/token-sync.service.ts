import { Injectable, OnDestroy } from '@angular/core';

import { BehaviorSubject, Subject, distinctUntilChanged } from 'rxjs';

import { BroadcastChannel, type LeaderElector, createLeaderElection } from 'broadcast-channel';

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

type AuthMessage = LoginMessage | LogoutMessage | TokenUpdatedMessage;

const tabId = generateUuidv4();

@Injectable({ providedIn: 'root' })
export class TokenSyncService implements OnDestroy {
  private readonly loginMessageSubject = new Subject<LoginMessage>();
  private readonly logoutMessageSubject = new Subject<LogoutMessage>();
  private readonly newTokenMessageSubject = new Subject<TokenUpdatedMessage>();
  private readonly isLeaderSubject = new BehaviorSubject<boolean>(false);
  readonly isLeader$ = this.isLeaderSubject.asObservable().pipe(distinctUntilChanged());

  private readonly channel: BroadcastChannel<AuthMessage> | null;
  private readonly leader: LeaderElector | null;
  private destroyed = false;

  public onLoginMessage = this.loginMessageSubject.asObservable();
  public onLogoutMessage = this.logoutMessageSubject.asObservable();
  public newTokenMessage = this.newTokenMessageSubject.asObservable();

  constructor() {
    let channel: BroadcastChannel<AuthMessage> | null = null;
    let leader: LeaderElector | null = null;

    if (typeof window === 'undefined') {
      this.channel = channel;
      this.leader = leader;
      this.isLeaderSubject.next(true);
      return;
    }

    try {
      channel = new BroadcastChannel<AuthMessage>('auth');
      leader = createLeaderElection(channel);
      leader.onduplicate = () => {
        console.warn('[auth-sync] duplicate leader detected');
      };
    } catch (error) {
      console.warn('[auth-sync] unable to initialize cross-tab leader election', error);
      void channel?.close().catch(() => undefined);
      this.channel = null;
      this.leader = null;
      this.isLeaderSubject.next(true);
      return;
    }

    this.channel = channel;
    this.leader = leader;

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

    void this.awaitLeadership();
  }

  isLeaderSnapshot(): boolean {
    return this.isLeaderSubject.value;
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.loginMessageSubject.complete();
    this.logoutMessageSubject.complete();
    this.newTokenMessageSubject.complete();
    this.isLeaderSubject.complete();

    const channel = this.channel;
    const leader = this.leader;

    if (channel != null) {
      channel.onmessage = null;
    }

    void this.dispose(channel, leader);
  }

  async newLogin(user: UserResponseDto, token: string) {
    return this.postLoginMessage(token, user);
  }

  private postLoginMessage(token: string, user: UserResponseDto) {
    return this.channel?.postMessage({ type: 'login', user, token, tabId });
  }

  postLogoutMessage() {
    return this.channel?.postMessage({ type: 'logout', tabId });
  }

  postTokenUpdatedMessage(token: string) {
    return this.channel?.postMessage({ type: 'token-updated', token, tabId });
  }

  private async awaitLeadership(): Promise<void> {
    if (this.leader == null) {
      return;
    }

    try {
      await this.leader.awaitLeadership();

      if (!this.destroyed) {
        this.isLeaderSubject.next(true);
      }
    } catch (error) {
      if (!this.destroyed) {
        console.warn('[auth-sync] leader election failed', error);
        this.isLeaderSubject.next(true);
      }
    }
  }

  private async dispose(channel: BroadcastChannel<AuthMessage> | null, leader: LeaderElector | null): Promise<void> {
    try {
      await leader?.die();
    } catch {
      // Ignore teardown failures during tab close/unload.
    }

    try {
      await channel?.close();
    } catch {
      // Ignore teardown failures during tab close/unload.
    }
  }
}
