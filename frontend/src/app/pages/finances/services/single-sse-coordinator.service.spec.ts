// @vitest-environment jsdom
import { BroadcastChannel, type LeaderElector, enforceOptions } from 'broadcast-channel';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { SingleSseCoordinatorService } from './single-sse-coordinator.service';

type CoordinatorInternals = {
  channel: BroadcastChannel<unknown> | null;
  elector: LeaderElector | null;
};

async function settleCoordinator(ms = 50) {
  await vi.advanceTimersByTimeAsync(ms);
}

describe('SingleSseCoordinatorService', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    enforceOptions({ type: 'simulate' });
  });

  afterEach(() => {
    enforceOptions(null);
    vi.useRealTimers();
  });

  it('elects exactly one leader and performs failover when leader closes', async () => {
    const tabA = new SingleSseCoordinatorService();
    const tabB = new SingleSseCoordinatorService();

    await settleCoordinator();

    const leaders = [tabA, tabB].filter(tab => tab.isLeaderSnapshot());
    expect(leaders).toHaveLength(1);

    const leader = leaders[0];
    const follower = leader === tabA ? tabB : tabA;

    leader.ngOnDestroy();
    await settleCoordinator();

    expect(follower.isLeaderSnapshot()).toBe(true);

    follower.ngOnDestroy();
    await settleCoordinator();
  });

  it('broadcasts events from the leader and deduplicates repeated sourceEventId', async () => {
    const tabA = new SingleSseCoordinatorService();
    const tabB = new SingleSseCoordinatorService();

    await settleCoordinator();

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

    await settleCoordinator();

    expect(received).toEqual(['evt-1']);

    tabA.ngOnDestroy();
    tabB.ngOnDestroy();
    await settleCoordinator();
  });

  it('does not forward events when this tab is not the leader', async () => {
    const tabA = new SingleSseCoordinatorService();
    const tabB = new SingleSseCoordinatorService();

    await settleCoordinator();

    const follower = tabA.isLeaderSnapshot() ? tabB : tabA;
    const peer = follower === tabA ? tabB : tabA;
    const received: string[] = [];

    peer.distributedEvent$.subscribe(event => {
      received.push(event.sourceEventId);
    });

    follower.forwardEvent({
      event: 'WALLET_EVENT',
      data: '{"id":"tx-1"}',
      sourceEventId: 'evt-1',
    });

    await settleCoordinator();

    expect(received).toEqual([]);

    tabA.ngOnDestroy();
    tabB.ngOnDestroy();
    await settleCoordinator();
  });

  it('releases the leader elector and closes the channel on destroy', async () => {
    const service = new SingleSseCoordinatorService();

    await settleCoordinator();

    const { channel, elector } = service as unknown as CoordinatorInternals;

    service.ngOnDestroy();
    await settleCoordinator();

    expect(channel?.isClosed).toBe(true);
    expect(elector?.isDead).toBe(true);
  });
});
