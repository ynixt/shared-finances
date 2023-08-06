import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { TitleGuard } from "../../shared";
import { SettingsPageComponent } from "./settings-page.component";

const routes: Routes = [
  {
    path: "",
    component: SettingsPageComponent,
    canActivate: [TitleGuard],
    data: {
      title: "settings"
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SettingsPageRoutingModule {
}
