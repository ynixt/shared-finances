import { Component, OnDestroy, OnInit } from "@angular/core";
import { AuthDispatchers } from "./store";
import { AuthSelectors } from "./store/services/selectors";
import { StompService } from "./@core/services/stomp.service";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
  styleUrls: ["./app.component.scss"]
})
@UntilDestroy()
export class AppComponent implements OnInit, OnDestroy {
  title = "shared-finances";
  appDone: boolean;

  constructor(private authDispatchers: AuthDispatchers, private authSelectors: AuthSelectors, private stompService: StompService) {
  }

  ngOnInit(): void {
    this.authDispatchers.getCurrentUser();
    this.authSelectors.done().then(done => (this.appDone = done));

    this.authSelectors.state$.pipe(untilDestroyed(this)).subscribe(authState => {
      if (authState.done) {
        if (authState.user) {
          this.stompService.start();
        } else {
          this.stompService.stop();
        }
      }
    });
  }

  async ngOnDestroy() {
    await this.stompService.ngOnDestroy();
  }
}
