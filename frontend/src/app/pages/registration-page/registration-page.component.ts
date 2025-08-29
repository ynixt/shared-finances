import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Toast } from 'primeng/toast';

import { LanguagePickerComponent } from '../../components/language-picker/language-picker.component';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { KratosAuthService } from '../../services/kratos-auth.service';
import { DEFAULT_ERROR_LIFE } from '../../util/error-util';
import { translateKratosError } from '../../util/kratos-i18n';
import { DEFAULT_SUCCESS_LIFE } from '../../util/success-util';
import { promiseTimeout } from '../../util/timeout-util';
import { confirmPasswordValidator } from './confirm-password.validator';

@Component({
  selector: 'app-registration-page',
  imports: [ReactiveFormsModule, Button, Password, InputText, TranslatePipe, Toast, LanguagePickerComponent, NavbarComponent],
  templateUrl: './registration-page.component.html',
  styleUrl: './registration-page.component.scss',
  providers: [MessageService],
})
export class RegistrationPageComponent implements OnInit {
  form!: FormGroup;
  flow: any;
  submitting = false;

  constructor(
    private fb: FormBuilder,
    private auth: KratosAuthService,
    private translateService: TranslateService,
    private messageService: MessageService,
    private router: Router,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      await this.getFlow();
    } catch (error) {
      await this.getFlow();
    }
  }

  async submit(): Promise<void> {
    if (!this.flow || this.form.invalid || this.submitting) {
      return;
    }

    const body = {
      method: 'password',
      csrf_token: this.form.value.csrf_token,
      password: this.form.value.password,
      'traits.email': this.form.value.email,
      'traits.lang': this.form.value.language,
      'traits.name.firstName': this.form.value.name.first,
      'traits.name.lastName': this.form.value.name.last,
    };

    this.submitting = true;

    try {
      await this.auth.submitRegistrationFlow(this.flow.id, body);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('registerPage.success.title'),
        detail: this.translateService.instant('registerPage.success.message'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await promiseTimeout(DEFAULT_SUCCESS_LIFE);
      await this.router.navigateByUrl('/login');
    } catch (error) {
      this.submitting = false;

      console.error(error);

      let errorMessage = translateKratosError(error, this.translateService);

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
    const flow = await this.auth.getRegistrationFlow();

    this.flow = flow;

    const csrf = flow.ui.nodes.find((n: any) => n.attributes.name === 'csrf_token')?.attributes.value;

    this.form = this.fb.group(
      {
        email: ['', [Validators.required, Validators.email]],
        name: this.fb.group({
          first: ['', [Validators.required, Validators.minLength(2)]],
          last: ['', [Validators.required, Validators.minLength(2)]],
        }),
        password: ['', [Validators.required, Validators.minLength(3)]],
        confirmPassword: ['', [Validators.required]],
        language: ['', [Validators.required]],
        method: ['password'],
        csrf_token: [csrf],
      },
      { validators: confirmPasswordValidator },
    );
  }
}
