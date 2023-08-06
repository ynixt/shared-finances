import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";

import { SettingsPageRoutingModule } from "./settings-page-routing.module";
import { SettingsPageComponent } from "./settings-page.component";
import { MatOptionModule } from "@angular/material/core";
import { MatSelectModule } from "@angular/material/select";
import { TranslocoModule } from "@ngneat/transloco";
import { FormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { SettingsService } from "./settings.service";


@NgModule({
  declarations: [
    SettingsPageComponent
  ],
  providers: [
    SettingsService
  ],
  imports: [
    CommonModule,
    SettingsPageRoutingModule,
    MatOptionModule,
    MatSelectModule,
    TranslocoModule,
    FormsModule,
    MatButtonModule
  ]
})
export class SettingsPageModule {
}
