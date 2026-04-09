import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { SingleSseCoordinatorService } from './single-sse-coordinator.service';

const TAB_STORAGE_KEY = 'sf-user-events-tab-id-v1';

class FakeBroadcastChannel {
  private static readonly channels = new Map<string, Set<FakeBroadcastChannel>>();

  static reset() {
    this.channels.clear();
  }

  onmessage: ((event: MessageEvent<any>) => void) | null = null;
  private closed = false;

  constructor(private readonly name: string) {
    if (!FakeBroadcastChannel.channels.has(name)) {
      FakeBroadcastChannel.channels.set(name, new Set());
    }

    FakeBroadcastChannel.channels.get(name)!.add(this);
  }

  postMessage(data: unknown) {
    if (this.closed) {
      return;
    }

    const peers = FakeBroadcastChannel.channels.get(this.name);
    if (peers == null) {
      return;
    }

    for (const peer of peers) {
      if (peer === this || peer.closed) {
        continue;
      }
      peer.onmessage?.({ data } as MessageEvent<any>);
    }
  }

  close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
    FakeBroadcastChannel.channels.get(this.name)?.delete(this);
  }
}

describe('SingleSseCoordinatorService', () => {
  function createDistinctTabs() {
    const tabA = new SingleSseCoordinatorService();
    // In real browsers, each tab has isolated sessionStorage. In jsdom tests we must emulate that isolation.
    window.sessionStorage.removeItem(TAB_STORAGE_KEY);
    const tabB = new SingleSseCoordinatorService();
    return { tabA, tabB };
  }

  beforeEach(() => {
    vi.useFakeTimers();
    FakeBroadcastChannel.reset();
    window.sessionStorage.clear();

    Object.defineProperty(window, 'BroadcastChannel', {
      configurable: true,
      writable: true,
      value: FakeBroadcastChannel,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('elects exactly one leader and performs failover when leader closes', () => {
    const { tabA, tabB } = createDistinctTabs();

    vi.advanceTimersByTime(1_000);

    const leaders = [tabA, tabB].filter(tab => tab.isLeaderSnapshot());
    expect(leaders).toHaveLength(1);

    const leader = leaders[0];
    const follower = leader === tabA ? tabB : tabA;

    leader.ngOnDestroy();
    vi.advanceTimersByTime(5_000);

    expect(follower.isLeaderSnapshot()).toBe(true);

    follower.ngOnDestroy();
  });

  it('broadcasts events from leader and deduplicates repeated sourceEventId', () => {
    const { tabA, tabB } = createDistinctTabs();

    vi.advanceTimersByTime(1_000);

    const leader = tabA.isLeaderSnapshot() ? tabA : tabB;
    const follower = leader === tabA ? tabB : tabA;

    const received: string[] = [];
    follower.distributedEvent$.subscribe(event => {
      received.push(event.sourceEventId);
    });

    leader.forwardEvent({
      event: 'WALLET_EVENT',
      data: '{"id":"tx-1"}',
      sourceEventId: 'evt-1',
    });

    leader.forwardEvent({
      event: 'WALLET_EVENT',
      data: '{"id":"tx-1"}',
      sourceEventId: 'evt-1',
    });

    expect(received).toEqual(['evt-1']);

    tabA.ngOnDestroy();
    tabB.ngOnDestroy();
  });

  it('emits resync-required after leadership handoff', () => {
    const { tabA, tabB } = createDistinctTabs();

    vi.advanceTimersByTime(1_000);

    const leader = tabA.isLeaderSnapshot() ? tabA : tabB;
    const follower = leader === tabA ? tabB : tabA;

    let resyncCount = 0;
    follower.resyncRequired$.subscribe(() => {
      resyncCount++;
    });

    leader.ngOnDestroy();
    vi.advanceTimersByTime(5_000);

    expect(resyncCount).toBeGreaterThanOrEqual(1);
    expect(follower.isLeaderSnapshot()).toBe(true);

    follower.ngOnDestroy();
  });
});
