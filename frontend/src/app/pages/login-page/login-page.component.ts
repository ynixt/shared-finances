import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { KratosAuthService } from '../../services/kratos-auth.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';
import { translateKratosError } from '../../util/kratos-i18n';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, Toast],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
  providers: [MessageService],
})
export class LoginPageComponent implements OnInit {
  form!: FormGroup;
  flow: any;

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private auth: KratosAuthService,
    private router: Router,
    private translateService: TranslateService,
    private messageService: MessageService,
  ) {}

  async ngOnInit(): Promise<void> {
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

    try {
      await this.auth.submitLoginFlow(this.flow.id, this.form.value);
      await this.auth.refreshJwt();

      await this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('return_to') ?? '/dashboard');
    } catch (err) {
      let errorMessage = translateKratosError(err, this.translateService);

      if (errorMessage == null) {
        errorMessage = this.translateService.instant('registerPage.error.genericMessage');
      }

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('registerPage.error.title'),
        detail: errorMessage!!,
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }

  private async getFlow() {
    const flow = await this.auth.getLoginFlow(this.route.snapshot.queryParamMap.get('return_to') ?? '/');

    this.flow = flow;

    const csrf = flow.ui.nodes.find((n: any) => n.attributes.name === 'csrf_token')?.attributes.value;

    this.form = this.fb.group({
      identifier: ['', [Validators.required]],
      password: ['', [Validators.required]],
      method: ['password'],
      csrf_token: [csrf],
    });
  }
}
