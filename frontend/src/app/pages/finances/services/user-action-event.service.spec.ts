import { NgZone } from '@angular/core';

import { BehaviorSubject, Subject, of } from 'rxjs';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../../../environments/environment';
import { TokenStateService } from '../../../services/token-state.service';
import { SingleSseCoordinatorService, type SseCoordinatorState } from './single-sse-coordinator.service';
import { UserActionEventService } from './user-action-event.service';

const eventSourceHarness = vi.hoisted(() => {
  return {
    handlers: [] as Array<{ onMessage?: (event: { id?: string; event?: string; data: string }) => void }>,
    closes: [] as Array<ReturnType<typeof vi.fn>>,
  };
});

vi.mock('eventsource-client', () => ({
  createEventSource: (options: { onMessage?: (event: { id?: string; event?: string; data: string }) => void }) => {
    eventSourceHarness.handlers.push(options);
    const close = vi.fn();
    eventSourceHarness.closes.push(close);
    return { close };
  },
}));

type CoordinatorMock = {
  isSupported: boolean;
  state$: BehaviorSubject<SseCoordinatorState>;
  isLeader$: BehaviorSubject<boolean>;
  distributedEvent$: Subject<{ event?: string; data: string; sourceEventId: string }>;
  forwardEvent: ReturnType<typeof vi.fn>;
};

function buildCoordinatorMock(initialState: SseCoordinatorState = 'follower'): CoordinatorMock {
  return {
    isSupported: true,
    state$: new BehaviorSubject<SseCoordinatorState>(initialState),
    isLeader$: new BehaviorSubject<boolean>(initialState === 'leader'),
    distributedEvent$: new Subject<{ event?: string; data: string; sourceEventId: string }>(),
    forwardEvent: vi.fn(),
  };
}

function setCoordinatorState(coordinator: CoordinatorMock, state: SseCoordinatorState) {
  coordinator.state$.next(state);
  coordinator.isLeader$.next(state === 'leader');
}

function buildZoneMock() {
  return {
    run: (fn: () => void) => fn(),
  };
}

async function flushAsyncWork() {
  await Promise.resolve();
  await Promise.resolve();
}

describe('UserActionEventService', () => {
  const originalSingleSseFlag = environment.singleSsePerBrowser;

  beforeEach(() => {
    eventSourceHarness.handlers.length = 0;
    eventSourceHarness.closes.length = 0;
  });

  afterEach(() => {
    environment.singleSsePerBrowser = originalSingleSseFlag;
  });

  it('keeps transaction observable contract in direct SSE mode', async () => {
    environment.singleSsePerBrowser = false;
    const coordinator = buildCoordinatorMock();
    const service = new UserActionEventService(
      { token$: of('token-1') } as unknown as TokenStateService,
      buildZoneMock() as unknown as NgZone,
      coordinator as unknown as SingleSseCoordinatorService,
    );

    const inserted: string[] = [];
    const updated: string[] = [];
    const deleted: string[] = [];

    service.transactionInserted$.subscribe(event => inserted.push(event.id ?? ''));
    service.transactionUpdated$.subscribe(event => updated.push(event.id ?? ''));
    service.transactionDeleted$.subscribe(event => deleted.push(event.id ?? ''));

    await flushAsyncWork();
    const onMessage = eventSourceHarness.handlers[0]?.onMessage;
    expect(onMessage).toBeTruthy();

    onMessage?.({
      id: 'evt-insert',
      event: 'WALLET_EVENT',
      data: JSON.stringify({ id: 'evt-insert', type: 'INSERT', groupId: null, data: { id: 'tx-insert' } }),
    });
    onMessage?.({
      id: 'evt-update',
      event: 'WALLET_EVENT',
      data: JSON.stringify({ id: 'evt-update', type: 'UPDATE', groupId: null, data: { id: 'tx-update' } }),
    });
    onMessage?.({
      id: 'evt-delete',
      event: 'WALLET_EVENT',
      data: JSON.stringify({ id: 'evt-delete', type: 'DELETE', groupId: null, data: { id: 'tx-delete' } }),
    });

    await flushAsyncWork();

    expect(inserted).toEqual(['tx-insert']);
    expect(updated).toEqual(['tx-update']);
    expect(deleted).toEqual(['tx-delete']);
  });

  it('delivers transaction updates from cross-tab broadcast when follower', async () => {
    environment.singleSsePerBrowser = true;
    const coordinator = buildCoordinatorMock('follower');
    const service = new UserActionEventService(
      { token$: of('token-1') } as unknown as TokenStateService,
      buildZoneMock() as unknown as NgZone,
      coordinator as unknown as SingleSseCoordinatorService,
    );
    const updated: string[] = [];

    service.transactionUpdated$.subscribe(event => updated.push(event.id ?? ''));

    coordinator.distributedEvent$.next({
      sourceEventId: 'evt-update',
      event: 'WALLET_EVENT',
      data: JSON.stringify({ id: 'evt-update', type: 'UPDATE', groupId: null, data: { id: 'tx-update' } }),
    });

    await flushAsyncWork();

    expect(updated).toEqual(['tx-update']);
    expect(eventSourceHarness.handlers).toHaveLength(0);
  });

  it('forwards SSE events through coordinator when tab is leader', async () => {
    environment.singleSsePerBrowser = true;
    const coordinator = buildCoordinatorMock('leader');
    const service = new UserActionEventService(
      { token$: of('token-1') } as unknown as TokenStateService,
      buildZoneMock() as unknown as NgZone,
      coordinator as unknown as SingleSseCoordinatorService,
    );
    const updated: string[] = [];

    service.transactionUpdated$.subscribe(event => updated.push(event.id ?? ''));

    await flushAsyncWork();
    const onMessage = eventSourceHarness.handlers[0]?.onMessage;
    onMessage?.({
      id: 'evt-42',
      event: 'WALLET_EVENT',
      data: JSON.stringify({ id: 'evt-42', type: 'UPDATE', groupId: null, data: { id: 'tx-42' } }),
    });

    await flushAsyncWork();

    expect(updated).toEqual(['tx-42']);
    expect(coordinator.forwardEvent).toHaveBeenCalledWith({
      sourceEventId: 'evt-42',
      event: 'WALLET_EVENT',
      data: JSON.stringify({ id: 'evt-42', type: 'UPDATE', groupId: null, data: { id: 'tx-42' } }),
    });
  });

  it('emits resync when this tab acquires leadership', async () => {
    environment.singleSsePerBrowser = true;
    const coordinator = buildCoordinatorMock('follower');
    const service = new UserActionEventService(
      { token$: of('token-1') } as unknown as TokenStateService,
      buildZoneMock() as unknown as NgZone,
      coordinator as unknown as SingleSseCoordinatorService,
    );
    let count = 0;

    service.resyncRequired$.subscribe(() => {
      count++;
    });

    setCoordinatorState(coordinator, 'leader');

    await flushAsyncWork();

    expect(count).toBe(1);
  });
});
