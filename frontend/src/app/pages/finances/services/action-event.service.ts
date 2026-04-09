import { Injectable, NgZone, OnDestroy } from '@angular/core';

import { EMPTY, Observable, Subject, defer, filter, from, lastValueFrom, map, merge, share, switchMap, take, tap } from 'rxjs';

import { createEventSource } from 'eventsource-client';

import { environment } from '../../../../environments/environment';
import { ActionEventCategory } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { TokenStateService } from '../../../services/token-state.service';
import { SingleSseCoordinatorService } from './single-sse-coordinator.service';

type Wire = { id?: string; event?: string; data: string };

@Injectable()
export abstract class ActionEventService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();

  protected readonly wire$: Observable<Wire>;
  protected readonly resyncRequiredSignal$: Observable<void>;

  protected constructor(
    private tokenStateService: TokenStateService,
    private zone: NgZone,
    private sseCoordinator: SingleSseCoordinatorService,
    private sseUrl: string,
  ) {
    const sseConnection$ = defer(() =>
      from(lastValueFrom(this.tokenStateService.token$.pipe(take(1)))).pipe(
        switchMap(token => {
          if (!token) throw new Error('Missing token');

          return new Observable<Wire>(subscriber => {
            const es = createEventSource({
              url: this.sseUrl,
              headers: {
                Authorization: `Bearer ${token}`,
              },
              onDisconnect: () => {
                console.log(`sse: disconnected ${this.sseUrl}`);
              },
              onConnect: () => {
                console.log(`sse: connected ${this.sseUrl}`);
              },
              onScheduleReconnect: info => {
                console.log(`sse: reconnecting ${this.sseUrl} - ${JSON.stringify(info)}`);
              },
              onMessage: ({ id, event, data }) => {
                this.zone.run(() => subscriber.next({ id, event, data }));
              },
            });

            return () => {
              console.log(`sse: closing ${this.sseUrl}`);
              es.close();
            };
          });
        }),
      ),
    );

    const singleSseEnabled = environment.singleSsePerBrowser === true && this.sseCoordinator.isSupported;

    if (singleSseEnabled) {
      console.info(`[sse] single-sse coordinator enabled for ${this.sseUrl}`);

      const leaderSse$ = this.sseCoordinator.isLeader$.pipe(
        switchMap(isLeader => {
          if (!isLeader) {
            return EMPTY;
          }

          return sseConnection$.pipe(
            tap(wire => {
              this.sseCoordinator.forwardEvent({
                event: wire.event,
                data: wire.data,
                sourceEventId: this.resolveSourceEventId(wire),
              });
            }),
          );
        }),
      );

      const followerBroadcast$ = this.sseCoordinator.distributedEvent$.pipe(
        map(event => ({ id: event.sourceEventId, event: event.event, data: event.data })),
      );

      this.wire$ = merge(leaderSse$, followerBroadcast$).pipe(
        share({
          connector: () => new Subject<Wire>(),
          resetOnComplete: true,
          resetOnError: true,
          resetOnRefCountZero: true,
        }),
      );

      this.resyncRequiredSignal$ = this.sseCoordinator.resyncRequired$;
      return;
    }

    console.info(`[sse] single-sse coordinator disabled, using direct SSE for ${this.sseUrl}`);

    this.wire$ = sseConnection$.pipe(
      share({
        connector: () => new Subject<Wire>(),
        resetOnComplete: true,
        resetOnError: true,
        resetOnRefCountZero: true,
      }),
    );
    this.resyncRequiredSignal$ = EMPTY;
  }

  protected streamOf<T>(category: ActionEventCategory): Observable<T> {
    return this.wire$.pipe(
      filter(msg => msg.event === category),
      map(msg => JSON.parse(msg.data as any as string) as T),
    );
  }

  private resolveSourceEventId(wire: Wire): string {
    if (wire.id != null && wire.id.length > 0) {
      return wire.id;
    }

    return `${wire.event ?? 'message'}:${wire.data}`;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
