import { RxStomp, RxStompState } from "@stomp/rx-stomp";
import { Injectable, OnDestroy } from "@angular/core";
import { Auth, user, User as FirebaseUser } from "@angular/fire/auth";
import { lastValueFrom, Observable, takeUntil } from "rxjs";
import { filter, take } from "rxjs/operators";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { HotToastService } from "@ngneat/hot-toast";
import { TranslocoService } from "@ngneat/transloco";
import { environment } from "../../../environments/environment";

@Injectable()
@UntilDestroy()
export class StompService extends RxStomp implements OnDestroy {
  private readonly connectionErrorToastId = "connectionErrorToastId";
  private readonly connectionReestablishedToastId = "connectionReestablishedToastId";
  private user$: Observable<FirebaseUser>;
  private firstClosedState = true;
  private serverOffline = false;

  constructor(
    private auth: Auth,
    private toast: HotToastService,
    private translocoService: TranslocoService
  ) {
    super();
    this.user$ = user(this.auth).pipe();
  }

  start() {
    this.serverOffline = false;
    this.configure({
      brokerURL: environment.websocketUrl,
      beforeConnect: (async client => {
        const user = await lastValueFrom(this.user$.pipe(take(1)).pipe(take(1)));
        const token = await user.getIdToken();
        client.configure({
          connectHeaders: {
            "Authorization": `Bearer ${token}`
          }
        });
      })
    });

    this.connectionState$.pipe(
      untilDestroyed(this),
      takeUntil(
        this.user$.pipe(filter(u => u == null))
      )
    ).subscribe(state => {
      if (state == RxStompState.CLOSED) {
        if (this.firstClosedState) {
          this.firstClosedState = false;
        } else {
          this.serverOffline = true;
          this.toast.error(this.translocoService.translate("server-offline"), {
            id: this.connectionErrorToastId,
            autoClose: false,
            dismissible: false
          });
        }
      } else if (state == RxStompState.OPEN) {
        if (this.serverOffline) {
          this.toast.close(this.connectionErrorToastId);
          this.toast.success(this.translocoService.translate("connection-reestablished"), {
            id: this.connectionReestablishedToastId
          });
          this.serverOffline = false;
        }
      }
    });

    this.activate();
  }

  async stop() {
    await this.deactivate();
  }

  async ngOnDestroy() {
    await this.stop();
  }
}
