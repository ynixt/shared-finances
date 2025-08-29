import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, effect } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { filter } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Toast } from 'primeng/toast';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { KratosAuthService } from '../../services/kratos-auth.service';
import { UserService } from '../../services/user.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';
import { translateKratosError } from '../../util/kratos-i18n';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, Toast, TranslatePipe, NavbarComponent, ButtonDirective],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  providers: [MessageService],
})
@UntilDestroy()
export class LoginPageComponent implements OnInit {
  form!: FormGroup;
  flow: any;
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private auth: KratosAuthService,
    private router: Router,
    private translateService: TranslateService,
    private messageService: MessageService,
    userService: UserService,
  ) {
    effect(() => {
      const error = userService.error();

      if (error != null) {
        console.error(error);

        this.messageService.add({
          severity: 'error',
          summary: this.translateService.instant('loginPage.error.title'),
          detail: this.translateService.instant('error.genericMessage'),
          life: DEFAULT_ERROR_LIFE,
        });
      }
    });
  }

  async ngOnInit(): Promise<void> {
    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        untilDestroyed(this),
      )
      .subscribe((e: NavigationEnd) => {
        if (e.urlAfterRedirects === '/login') {
          this.initLoginForm();
        }
      });

    await this.initLoginForm();
  }

  private async initLoginForm() {
    this.loading = false;

    try {
      await this.getFlow();
    } catch (error) {
      await this.getFlow();
    }
  }

  async submit(): Promise<void> {
    if (!this.flow || this.form.invalid) {
      return;
    }

    this.loading = true;

    try {
      await this.auth.submitLoginFlow(this.flow.id, this.form.value);
      await this.auth.refreshJwt();

      await this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('return_to') ?? '/app');
    } catch (err) {
      this.loading = false;
      let errorMessage = translateKratosError(err, this.translateService);

      if (errorMessage == null) {
        errorMessage = this.translateService.instant('error.genericMessage');
      }

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('loginPage.error.title'),
        detail: errorMessage!!,
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }

  private async getFlow() {
    const flow = await this.auth.getLoginFlow(this.route.snapshot.queryParamMap.get('return_to') ?? '/');

    this.flow = flow;

    const csrf = flow.ui.nodes.find((n: any) => n.attributes.name === 'csrf_token')?.attributes.value;

    if (this.form == null) {
      this.form = this.fb.group({
        identifier: ['', [Validators.required]],
        password: ['', [Validators.required]],
        method: ['password'],
        csrf_token: [csrf],
      });
    } else {
      this.form.get('csrf_token')!!.setValue([csrf]);
    }
  }
}
