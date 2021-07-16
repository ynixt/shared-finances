import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthRoutingModule } from './auth-routing.module';
import { LoginComponent } from './login/login.component';
import { SharedModule } from 'src/app/shared';
import { MatRippleModule } from '@angular/material/core';
import { GoogleButtonComponent } from './login/google-button/google-button.component';

@NgModule({
  declarations: [LoginComponent, GoogleButtonComponent],
  imports: [CommonModule, AuthRoutingModule, SharedModule, MatRippleModule],
})
export class AuthModule {}
