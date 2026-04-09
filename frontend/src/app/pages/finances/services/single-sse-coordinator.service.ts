import { Injectable, OnDestroy } from '@angular/core';

import { BehaviorSubject, Observable, Subject, distinctUntilChanged, map } from 'rxjs';

export type SseCoordinatorState = 'follower' | 'candidate' | 'leader';

export type CrossTabWireEvent = {
  event?: string;
  data: string;
  sourceEventId: string;
};

type CrossTabMessage =
  | {
      type: 'heartbeat';
      tabId: string;
      leaderEpoch: number;
      sentAt: number;
    }
  | {
      type: 'claim';
      tabId: string;
      sentAt: number;
    }
  | {
      type: 'leader-elected';
      tabId: string;
      leaderEpoch: number;
      sentAt: number;
    }
  | {
      type: 'event';
      leaderTabId: string;
      leaderEpoch: number;
      sourceEventId: string;
      event?: string;
      data: string;
      sentAt: number;
    }
  | {
      type: 'resync-required';
      leaderTabId: string;
      leaderEpoch: number;
      sentAt: number;
    };

@Injectable({ providedIn: 'root' })
export class SingleSseCoordinatorService implements OnDestroy {
  private static readonly channelName = 'sf-user-events-v1';
  private static readonly tabStorageKey = 'sf-user-events-tab-id-v1';

  private readonly heartbeatIntervalMs = 1000;
  private readonly leaseTimeoutMs = 3500;
  private readonly claimWindowMs = 200;
  private readonly dedupeTtlMs = 120_000;
  private readonly resyncDebounceMs = 300;

  readonly isSupported = typeof window !== 'undefined' && typeof window.BroadcastChannel !== 'undefined';

  private readonly tabId = this.resolveTabId();
  private readonly channel = this.isSupported ? new BroadcastChannel(SingleSseCoordinatorService.channelName) : null;

  private readonly stateSubject = new BehaviorSubject<SseCoordinatorState>('follower');
  readonly state$ = this.stateSubject.asObservable();
  readonly isLeader$ = this.state$.pipe(
    map(state => state === 'leader'),
    distinctUntilChanged(),
  );

  private readonly distributedEventSubject = new Subject<CrossTabWireEvent>();
  readonly distributedEvent$ = this.distributedEventSubject.asObservable();

  private readonly resyncRequiredSubject = new Subject<void>();
  readonly resyncRequired$ = this.resyncRequiredSubject.asObservable();

  private leaderTabId: string | null = null;
  private leaderEpoch = 0;
  private lastHeartbeatAt = 0;

  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private electionTimer: ReturnType<typeof setTimeout> | null = null;
  private resyncTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly electionCandidates = new Set<string>();
  private readonly dedupeKeys = new Map<string, number>();

  constructor() {
    if (!this.isSupported || this.channel == null) {
      return;
    }

    this.channel.onmessage = event => this.handleCrossTabMessage(event.data as CrossTabMessage);
    this.heartbeatTimer = setInterval(() => this.onHeartbeatTick(), this.heartbeatIntervalMs);

    this.startElection('startup');
  }

  isLeaderSnapshot(): boolean {
    return this.stateSubject.value === 'leader';
  }

  forwardEvent(event: CrossTabWireEvent) {
    if (!this.isLeaderSnapshot() || this.channel == null) {
      return;
    }

    this.channel.postMessage({
      type: 'event',
      leaderTabId: this.tabId,
      leaderEpoch: this.leaderEpoch,
      sourceEventId: event.sourceEventId,
      event: event.event,
      data: event.data,
      sentAt: Date.now(),
    } satisfies CrossTabMessage);
  }

  ngOnDestroy(): void {
    this.clearHeartbeatTimer();
    this.clearElectionTimer();
    this.clearResyncTimer();

    this.channel?.close();
  }

  private resolveTabId(): string {
    if (typeof window === 'undefined') {
      return `ssr-${Math.random().toString(36).slice(2)}`;
    }

    const existing = window.sessionStorage.getItem(SingleSseCoordinatorService.tabStorageKey);
    if (existing != null && existing.length > 0) {
      return existing;
    }

    const created =
      typeof window.crypto?.randomUUID === 'function'
        ? window.crypto.randomUUID()
        : `tab-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    window.sessionStorage.setItem(SingleSseCoordinatorService.tabStorageKey, created);
    return created;
  }

  private onHeartbeatTick() {
    this.pruneDedupeKeys();

    if (this.isLeaderSnapshot()) {
      this.publishHeartbeat();
      return;
    }

    const now = Date.now();
    if (now - this.lastHeartbeatAt > this.leaseTimeoutMs) {
      this.startElection('lease-timeout');
    }
  }

  private startElection(reason: string) {
    if (!this.isSupported || this.channel == null) {
      return;
    }

    if (this.stateSubject.value === 'candidate') {
      return;
    }

    this.log(`election:start reason=${reason}`);
    this.stateSubject.next('candidate');
    this.electionCandidates.clear();
    this.electionCandidates.add(this.tabId);

    this.channel.postMessage({
      type: 'claim',
      tabId: this.tabId,
      sentAt: Date.now(),
    } satisfies CrossTabMessage);

    this.clearElectionTimer();
    this.electionTimer = setTimeout(() => this.finishElectionRound(), this.claimWindowMs);
  }

  private finishElectionRound() {
    this.clearElectionTimer();

    if (this.stateSubject.value !== 'candidate') {
      return;
    }

    const winner = [...this.electionCandidates].sort((a, b) => a.localeCompare(b))[0];
    this.electionCandidates.clear();

    if (winner === this.tabId) {
      this.promoteToLeader();
      return;
    }

    this.log(`election:lost winner=${winner}`);
    this.stateSubject.next('follower');
  }

  private promoteToLeader() {
    if (this.channel == null) {
      return;
    }

    const previousLeader = this.leaderTabId;
    this.leaderTabId = this.tabId;
    this.leaderEpoch = Math.max(this.leaderEpoch + 1, Date.now());
    this.lastHeartbeatAt = Date.now();

    this.stateSubject.next('leader');
    this.log(`leader:acquired epoch=${this.leaderEpoch}`);

    this.channel.postMessage({
      type: 'leader-elected',
      tabId: this.tabId,
      leaderEpoch: this.leaderEpoch,
      sentAt: Date.now(),
    } satisfies CrossTabMessage);

    this.publishHeartbeat();

    if (previousLeader !== this.tabId) {
      this.emitResyncRequiredLocal();
      this.channel.postMessage({
        type: 'resync-required',
        leaderTabId: this.tabId,
        leaderEpoch: this.leaderEpoch,
        sentAt: Date.now(),
      } satisfies CrossTabMessage);
    }
  }

  private publishHeartbeat() {
    if (!this.isLeaderSnapshot() || this.channel == null) {
      return;
    }

    this.channel.postMessage({
      type: 'heartbeat',
      tabId: this.tabId,
      leaderEpoch: this.leaderEpoch,
      sentAt: Date.now(),
    } satisfies CrossTabMessage);
  }

  private handleCrossTabMessage(message: CrossTabMessage) {
    switch (message.type) {
      case 'claim':
        if (this.stateSubject.value === 'candidate') {
          this.electionCandidates.add(message.tabId);
        }
        return;

      case 'heartbeat':
        this.observeLeader(message.tabId, message.leaderEpoch, false);
        return;

      case 'leader-elected':
        this.observeLeader(message.tabId, message.leaderEpoch, true);
        return;

      case 'event':
        this.handleDistributedEvent(message);
        return;

      case 'resync-required':
        if (message.leaderTabId !== this.tabId) {
          this.emitResyncRequiredLocal();
        }
        return;
    }
  }

  private handleDistributedEvent(message: Extract<CrossTabMessage, { type: 'event' }>) {
    if (message.leaderTabId === this.tabId) {
      return;
    }

    this.observeLeader(message.leaderTabId, message.leaderEpoch, false);

    const dedupeKey = `${message.leaderEpoch}:${message.sourceEventId}`;
    if (!this.shouldProcessDedupeKey(dedupeKey)) {
      return;
    }

    this.distributedEventSubject.next({
      event: message.event,
      data: message.data,
      sourceEventId: message.sourceEventId,
    });
  }

  private observeLeader(incomingLeaderTabId: string, incomingLeaderEpoch: number, isLeaderAnnouncement: boolean) {
    const currentLeaderTabId = this.leaderTabId;
    const currentLeaderEpoch = this.leaderEpoch;

    const hasNoLeader = currentLeaderTabId == null;
    const isSameLeader = currentLeaderTabId === incomingLeaderTabId && currentLeaderEpoch === incomingLeaderEpoch;
    const incomingIsStronger = this.isIncomingLeaderStronger(incomingLeaderTabId, incomingLeaderEpoch);

    if (!hasNoLeader && !isSameLeader && !incomingIsStronger) {
      return;
    }

    const leadershipChanged = !isSameLeader;

    this.leaderTabId = incomingLeaderTabId;
    this.leaderEpoch = incomingLeaderEpoch;
    this.lastHeartbeatAt = Date.now();

    if (incomingLeaderTabId !== this.tabId) {
      if (this.stateSubject.value !== 'follower') {
        this.log(`leader:demoted by=${incomingLeaderTabId} epoch=${incomingLeaderEpoch}`);
      }
      this.stateSubject.next('follower');
      this.clearElectionTimer();
    }

    if (isLeaderAnnouncement && leadershipChanged) {
      this.emitResyncRequiredLocal();
    }
  }

  private isIncomingLeaderStronger(incomingLeaderTabId: string, incomingLeaderEpoch: number): boolean {
    if (this.leaderTabId == null) {
      return true;
    }

    if (incomingLeaderEpoch !== this.leaderEpoch) {
      return incomingLeaderEpoch > this.leaderEpoch;
    }

    return incomingLeaderTabId.localeCompare(this.leaderTabId) < 0;
  }

  private shouldProcessDedupeKey(key: string): boolean {
    const now = Date.now();
    const expireAt = this.dedupeKeys.get(key);

    if (expireAt != null && expireAt > now) {
      return false;
    }

    this.dedupeKeys.set(key, now + this.dedupeTtlMs);
    return true;
  }

  private pruneDedupeKeys() {
    const now = Date.now();

    for (const [key, expireAt] of this.dedupeKeys.entries()) {
      if (expireAt <= now) {
        this.dedupeKeys.delete(key);
      }
    }
  }

  private emitResyncRequiredLocal() {
    this.clearResyncTimer();
    this.resyncTimer = setTimeout(() => this.resyncRequiredSubject.next(), this.resyncDebounceMs);
  }

  private clearHeartbeatTimer() {
    if (this.heartbeatTimer == null) {
      return;
    }

    clearInterval(this.heartbeatTimer);
    this.heartbeatTimer = null;
  }

  private clearElectionTimer() {
    if (this.electionTimer == null) {
      return;
    }

    clearTimeout(this.electionTimer);
    this.electionTimer = null;
  }

  private clearResyncTimer() {
    if (this.resyncTimer == null) {
      return;
    }

    clearTimeout(this.resyncTimer);
    this.resyncTimer = null;
  }

  private log(message: string) {
    console.debug(`[sse-coordinator] tab=${this.tabId} state=${this.stateSubject.value} ${message}`);
  }
}
