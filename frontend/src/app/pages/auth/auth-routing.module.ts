import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TitleGuard } from 'src/app/shared';
import { LoginComponent } from './login/login.component';

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [TitleGuard],
    data: {
      title: 'login',
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AuthRoutingModule {}
