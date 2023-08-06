import { Component, OnInit } from "@angular/core";
import { TranslocoService } from "@ngneat/transloco";
import { availableLanguages } from "../../@core/i18n";
import { AuthSelectors } from "../../store/services/selectors";
import { SettingsService } from "./settings.service";
import { UserSettings } from "../../@core/models";
import { take } from "rxjs/operators";
import { HotToastService } from "@ngneat/hot-toast";
import { ErrorService } from "../../@core/services";
import { lastValueFrom } from "rxjs";
import { TdDialogService } from "@covalent/core/dialogs";
import { AuthDispatchers } from "../../store";
import { Router } from "@angular/router";

@Component({
  selector: "app-settings-page",
  templateUrl: "./settings-page.component.html",
  styleUrls: ["./settings-page.component.scss"]
})
export class SettingsPageComponent implements OnInit {
  languages = availableLanguages;
  currentLang: string;

  constructor(
    private translocoService: TranslocoService,
    private authSelectors: AuthSelectors,
    private settingsService: SettingsService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private dialogService: TdDialogService,
    private authDispatchers: AuthDispatchers,
    private router: Router
  ) {
  }

  async ngOnInit() {
    const user = await this.authSelectors.currentUser();

    this.currentLang = user.lang;
  }

  async updateSettings(): Promise<void> {
    const newSettings: UserSettings = {
      lang: this.currentLang
    };

    await lastValueFrom(this.settingsService
      .updateSettings(newSettings)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate("creating"),
          success: this.translocoService.translate("settings-save-successful"),
          error: error =>
            this.errorService.getInstantErrorMessage(error, "settings-save-error", "settings-save-error-with-description")
        })
      )
    );

    await this.settingsService.updateSettings(newSettings);

    window.location.reload();
  }

  async confirmDeleteAccount() {
    const confirm = await this.dialogService
      .openConfirm({
        title: this.translocoService.translate("confirm"),
        message: this.translocoService.translate("delete-account-confirm"),
        cancelButton: this.translocoService.translate("cancel"),
        acceptButton: this.translocoService.translate("delete"),
        width: "500px"
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();
    if (confirm) {
      await this.deleteAccount();
    }
  }

  private async deleteAccount(): Promise<void> {
    await lastValueFrom(this.settingsService
      .deleteUser()
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate("deleting"),
          error: error =>
            this.errorService.getInstantErrorMessage(error, "delete-account-error", "delete-account-error-with-description")
        })
      ));
    await this.authDispatchers.logout();
    await this.router.navigateByUrl("/auth/login");
    window.location.reload();
  }
}
