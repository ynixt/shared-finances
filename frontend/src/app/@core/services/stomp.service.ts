import { RxStomp } from "@stomp/rx-stomp";
import { Injectable, OnDestroy } from "@angular/core";
import { Auth, user, User as FirebaseUser } from "@angular/fire/auth";
import { lastValueFrom, Observable } from "rxjs";
import { take } from "rxjs/operators";

@Injectable()
export class StompService extends RxStomp implements OnDestroy {
  private user$: Observable<FirebaseUser>;

  constructor(private auth: Auth) {
    super();
    this.user$ = user(this.auth).pipe();
  }

  async start() {
    const user = await lastValueFrom(this.user$.pipe(take(1)).pipe(take(1)));
    const token = await user.getIdToken();

    this.configure({
      brokerURL: "ws://localhost:8080/api/socket",
      connectHeaders: { "Authorization": `Bearer ${token}` }

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
