import { Injectable, OnDestroy } from '@angular/core';

import { BehaviorSubject, Subject, distinctUntilChanged, map } from 'rxjs';

import { BroadcastChannel, type LeaderElector, createLeaderElection } from 'broadcast-channel';

export type SseCoordinatorState = 'follower' | 'leader';

export type CrossTabWireEvent = {
  event?: string;
  data: string;
  sourceEventId: string;
};

type CrossTabMessage = {
  type: 'event';
  event?: string;
  data: string;
  sourceEventId: string;
};

@Injectable({ providedIn: 'root' })
export class SingleSseCoordinatorService implements OnDestroy {
  private static readonly channelName = 'sf-user-events-v1';
  private static readonly dedupeTtlMs = 120_000;

  readonly isSupported: boolean;

  private readonly stateSubject = new BehaviorSubject<SseCoordinatorState>('follower');
  readonly state$ = this.stateSubject.asObservable();
  readonly isLeader$ = this.state$.pipe(
    map(state => state === 'leader'),
    distinctUntilChanged(),
  );

  private readonly distributedEventSubject = new Subject<CrossTabWireEvent>();
  readonly distributedEvent$ = this.distributedEventSubject.asObservable();

  private readonly seenEventExpirations = new Map<string, number>();

  private channel: BroadcastChannel<CrossTabMessage> | null = null;
  private elector: LeaderElector | null = null;
  private destroyed = false;

  constructor() {
    let channel: BroadcastChannel<CrossTabMessage> | null = null;
    let elector: LeaderElector | null = null;

    if (typeof window !== 'undefined') {
      try {
        channel = new BroadcastChannel<CrossTabMessage>(SingleSseCoordinatorService.channelName);
        elector = createLeaderElection(channel);
      } catch (error) {
        console.warn('[sse-coordinator] unable to initialize cross-tab coordinator', error);
        void channel?.close().catch(() => undefined);
      }
    }

    this.isSupported = channel != null && elector != null;
    this.channel = channel;
    this.elector = elector;

    if (this.channel == null || this.elector == null) {
      return;
    }

    this.channel.onmessage = message => this.handleIncomingMessage(message);
    this.elector.onduplicate = () => {
      console.warn('[sse-coordinator] duplicate leader detected');
    };

    void this.awaitLeadership();
  }

  isLeaderSnapshot(): boolean {
    return this.stateSubject.value === 'leader';
  }

  forwardEvent(event: CrossTabWireEvent): void {
    if (!this.isLeaderSnapshot() || this.channel == null) {
      return;
    }

    void this.channel
      .postMessage({
        type: 'event',
        event: event.event,
        data: event.data,
        sourceEventId: event.sourceEventId,
      })
      .catch(error => {
        if (!this.destroyed) {
          console.warn('[sse-coordinator] unable to forward cross-tab event', error);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.stateSubject.next('follower');
    this.stateSubject.complete();
    this.distributedEventSubject.complete();
    this.seenEventExpirations.clear();

    const channel = this.channel;
    const elector = this.elector;

    this.channel = null;
    this.elector = null;

    if (channel != null) {
      channel.onmessage = null;
    }

    void this.dispose(channel, elector);
  }

  private async awaitLeadership(): Promise<void> {
    if (this.elector == null) {
      return;
    }

    try {
      await this.elector.awaitLeadership();

      if (this.destroyed) {
        return;
      }

      this.stateSubject.next('leader');
    } catch (error) {
      if (!this.destroyed) {
        console.warn('[sse-coordinator] leader election failed', error);
      }
    }
  }

  private handleIncomingMessage(message: CrossTabMessage): void {
    if (this.isLeaderSnapshot()) {
      return;
    }

    if (!this.shouldEmitEvent(message.sourceEventId)) {
      return;
    }

    this.distributedEventSubject.next({
      event: message.event,
      data: message.data,
      sourceEventId: message.sourceEventId,
    });
  }

  private shouldEmitEvent(sourceEventId: string): boolean {
    const now = Date.now();
    this.pruneSeenEvents(now);

    const expireAt = this.seenEventExpirations.get(sourceEventId);
    if (expireAt != null && expireAt > now) {
      return false;
    }

    this.seenEventExpirations.set(sourceEventId, now + SingleSseCoordinatorService.dedupeTtlMs);
    return true;
  }

  private pruneSeenEvents(now: number): void {
    for (const [key, expireAt] of this.seenEventExpirations.entries()) {
      if (expireAt <= now) {
        this.seenEventExpirations.delete(key);
      }
    }
  }

  private async dispose(channel: BroadcastChannel<CrossTabMessage> | null, elector: LeaderElector | null): Promise<void> {
    try {
      await elector?.die();
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
