import { Injectable, NgZone, OnDestroy } from '@angular/core';

import { Observable, Subject, defer, filter, from, map, share, switchMap } from 'rxjs';

import { createEventSource } from 'eventsource-client';

import { ActionEventCategory } from '../../../models/generated/com/ynixt/sharedfinances/domain/enums';
import { KratosAuthService } from '../../../services/kratos-auth.service';

type Wire = { event?: string; data: string };

@Injectable()
export abstract class ActionEventService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();

  protected readonly wire$: Observable<Wire>;

  protected constructor(
    private authService: KratosAuthService,
    private zone: NgZone,
    private sseUrl: string,
  ) {
    this.wire$ = defer(() =>
      from(this.authService.getToken()).pipe(
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
              onMessage: ({ event, data }) => {
                this.zone.run(() => subscriber.next({ event, data }));
              },
            });

            return () => {
              console.log(`sse: closing ${this.sseUrl}`);
              es.close();
            };
          });
        }),
      ),
    ).pipe(
      share({
        connector: () => new Subject<Wire>(),
        resetOnComplete: true,
        resetOnError: true,
        resetOnRefCountZero: true,
      }),
    );
  }

  protected streamOf<T>(category: ActionEventCategory): Observable<T> {
    return this.wire$.pipe(
      filter(msg => msg.event === category),
      map(msg => JSON.parse(msg.data as any as string) as T),
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
