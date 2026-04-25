import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';



import { QRCodeComponent } from 'angularx-qrcode';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { InputOtp } from 'primeng/inputotp';
import { InputText } from 'primeng/inputtext';
import { Panel } from 'primeng/panel';
import { Password } from 'primeng/password';
import { ProgressSpinner } from 'primeng/progressspinner';
import { TabsModule } from 'primeng/tabs';



import { CurrencySelectorComponent } from '../../../components/currency-selector/currency-selector.component';
import { LanguagePickerComponent } from '../../../components/language-picker/language-picker.component';
import { TimeZoneSelectorComponent } from '../../../components/timezone-selector/time-zone-selector.component';
import { UserAvatarEditorComponent } from '../../../components/user-avatar-editor/user-avatar-editor.component';
import { ConfirmMfaResponseDto, EnableMfaResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/auth/mfa';
import { UserResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { AuthService } from '../../../services/auth.service';
import { ErrorMessageService } from '../../../services/error-message.service';
import { UserService } from '../../../services/user.service';
import { DEFAULT_SUCCESS_LIFE } from '../../../util/success-util';
import { passwordValidator } from '../../registration-page/password-validator';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { confirmPasswordValidator } from './confirm-password.validator';




































@Component({
  selector: 'app-user-settings',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    LanguagePickerComponent,
    TabsModule,
    ReactiveFormsModule,
    ProgressSpinner,
    Password,
    Panel,
    Button,
    CurrencySelectorComponent,
    TimeZoneSelectorComponent,
    InputText,
    UserAvatarEditorComponent,
    Dialog,
    InputOtp,
    QRCodeComponent,
    ConfirmDialog,
  ],
  templateUrl: './user-settings.component.html',
  styleUrl: './user-settings.component.scss',
  providers: [ConfirmationService],
})
export class UserSettingsComponent {
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);

  user: UserResponseDto | null = null;
  submitting = false;

  generalForm = new FormGroup({
    email: new FormControl<string | undefined>(undefined, [Validators.required]),
    name: new FormGroup({
      first: new FormControl<string | undefined>(undefined, [Validators.required, Validators.minLength(2), Validators.maxLength(20)]),
      last: new FormControl<string | undefined>(undefined, [Validators.required, Validators.minLength(2), Validators.maxLength(20)]),
    }),
    language: new FormControl<string | undefined>(undefined, [Validators.required]),
    defaultCurrency: new FormControl<string | undefined>(undefined, [Validators.required]),
    tmz: new FormControl<string | undefined>(undefined, [Validators.required]),
    photo: new FormControl<string | null>(null, []),
  });

  securityPasswordForm = new FormGroup(
    {
      currentPassword: new FormControl<string | undefined>(undefined),
      newPassword: new FormControl<string | undefined>(undefined, [Validators.required, passwordValidator]),
      newPasswordConfirmation: new FormControl<string | undefined>(undefined, [Validators.required]),
    },
    { validators: confirmPasswordValidator },
  );

  securityTwoStepBeginForm = new FormGroup({
    currentPassword: new FormControl<string | undefined>(undefined, [Validators.required]),
    code: new FormControl<string | undefined>(undefined),
  });

  securityTwoStepConfirmForm = new FormGroup({
    code: new FormControl<string | undefined>(undefined, [Validators.required]),
  });

  showMfaConfirmDialog = false;
  enableMfaBeginResponse: EnableMfaResponseDto | undefined = undefined;
  enableMfaConfirmResponse: ConfirmMfaResponseDto | undefined = undefined;
  generateNewTwoStepValuesTimeout: any | undefined;

  constructor() {
    effect(() => {
      const user = this.userService.user();

      if (user != null) {
        this.generalForm.get('name')?.setValue({
          first: user.firstName,
          last: user.lastName,
        });
        this.generalForm.get('email')?.setValue(user.email);
        this.generalForm.get('language')?.setValue(user.lang);
        this.generalForm.get('defaultCurrency')?.setValue(user.defaultCurrency);
        this.generalForm.get('tmz')?.setValue(user.tmz);
        this.generalForm.get('photo')?.setValue(user.photoUrl != null ? user.photoUrl : null);

        const securityTwoStepCodeControl = this.securityTwoStepBeginForm.get('code');

        if (user.mfaEnabled) {
          securityTwoStepCodeControl?.setValidators([Validators.required]);
        } else {
          securityTwoStepCodeControl?.clearValidators();
        }
      }

      this.user = user;
    });
  }

  async submitGeneralForm() {
    if (this.generalForm.invalid || this.submitting) return;

    this.submitting = true;

    try {
      const photoUrl = this.generalForm.get('photo')!!.value;
      const getFromGravatar = photoUrl?.startsWith('https://www.gravatar.com/avatar/') ?? false;
      const photoAlreadyUploaded = photoUrl?.startsWith('/private/external/') ?? false;
      const removeAvatar = photoUrl === null;

      const photo = photoUrl == null || getFromGravatar || photoAlreadyUploaded ? undefined : await this.blobUrlToFile(photoUrl);

      await this.userService.updateCurrentUser(
        {
          firstName: this.generalForm.get('name')!!.value.first!!,
          lastName: this.generalForm.get('name')!!.value.last!!,
          defaultCurrency: this.generalForm.get('defaultCurrency')!!.value!!,
          email: this.generalForm.get('email')!!.value!!,
          tmz: this.generalForm.get('tmz')!!.value!!,
          lang: this.generalForm.get('language')!!.value!!,
          getFromGravatar,
          removeAvatar,
        },
        photo,
      );

      this.generalForm.reset();

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        life: DEFAULT_SUCCESS_LIFE,
      });
    } catch (err) {
      this.errorMessageService.handleError(err, this.messageService);
      throw err;
    } finally {
      this.submitting = false;
    }
  }

  private async blobUrlToFile(blobUrl: string): Promise<File> {
    const res = await fetch(blobUrl);
    if (!res.ok) throw new Error('Blob URL is not valid.');

    const blob = await res.blob();
    const type = blob.type || 'application/octet-stream';

    return new File([blob], 'avatar.png', { type });
  }

  async submitChangePasswordForm() {
    if (this.securityPasswordForm.invalid || this.submitting) return;

    this.submitting = true;

    try {
      await this.userService.changePassword({
        currentPassword: this.securityPasswordForm.get('currentPassword')!!.value!!,
        newPassword: this.securityPasswordForm.get('newPassword')!!.value!!,
      });

      this.securityPasswordForm.reset();

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.userSettingsPage.changePasswordSuccessfully'),
        life: DEFAULT_SUCCESS_LIFE,
      });
    } catch (err) {
      this.errorMessageService.handleError(err, this.messageService);
      throw err;
    } finally {
      this.submitting = false;
    }
  }

  async submitSecurityTwoStepBeginForm() {
    if (this.securityTwoStepBeginForm.invalid || this.submitting) return;

    this.clearGenerateNewTwoStepValuesTimeout();
    this.submitting = true;

    const rawPassword = this.securityTwoStepBeginForm.get('currentPassword')!!.value!!;

    try {
      if (this.user?.mfaEnabled) {
        await this.userService.disableMfa({
          rawPassword,
          code: this.securityTwoStepBeginForm.get('code')!!.value!!,
        });

        window.location.reload();
      } else {
        this.enableMfaBeginResponse = await this.userService.enableMfaBegin({
          rawPassword,
        });

        this.showMfaConfirmDialog = true;

        this.generateNewTwoStepValuesTimeout = setTimeout(
          () => {
            this.submitSecurityTwoStepBeginForm();
          },
          9.5 * 60 * 1000,
        );
      }
    } catch (err) {
      this.errorMessageService.handleError(err, this.messageService);

      throw err;
    } finally {
      this.submitting = false;
    }
  }

  async submitSecurityTwoStepConfirmForm() {
    if (this.securityTwoStepConfirmForm.invalid || this.submitting || !this.enableMfaBeginResponse) return;

    this.submitting = true;

    try {
      this.enableMfaConfirmResponse = await this.userService.enableMfaConfirm({
        code: this.securityTwoStepConfirmForm.get('code')!!.value!!,
        enrollmentId: this.enableMfaBeginResponse.enrollmentId,
      });

      this.enableMfaBeginResponse = undefined;

      this.securityTwoStepConfirmForm.reset();
      this.clearGenerateNewTwoStepValuesTimeout();
    } catch (err) {
      this.errorMessageService.handleError(err, this.messageService);

      if (err instanceof HttpErrorResponse && err.status === 401 && err.error.messageI18n === 'apiErrors.wrongMfaCode') {
        this.securityTwoStepConfirmForm.reset();
      } else {
        throw err;
      }
    } finally {
      this.submitting = false;
    }
  }

  closeTwoStepDialogConfirm(reloadPage: boolean = false) {
    this.showMfaConfirmDialog = false;
    this.enableMfaConfirmResponse = undefined;
    this.enableMfaBeginResponse = undefined;
    this.securityTwoStepBeginForm.reset();
    this.clearGenerateNewTwoStepValuesTimeout();

    if (reloadPage) {
      window.location.reload();
    }
  }

  clearGenerateNewTwoStepValuesTimeout() {
    clearTimeout(this.generateNewTwoStepValuesTimeout);
    this.generateNewTwoStepValuesTimeout = undefined;
  }

  confirmDeleteAccount() {
    this.confirmationService.confirm({
      message: this.translateService.instant('financesPage.userSettingsPage.deleteAccountConfirmMessage'),
      header: this.translateService.instant('financesPage.userSettingsPage.deleteAccountConfirmHeader'),
      closable: true,
      closeOnEscape: true,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.translateService.instant('financesPage.userSettingsPage.deleteAccount'),
      rejectLabel: this.translateService.instant('general.cancel'),
      acceptButtonProps: {
        severity: 'danger',
      },
      rejectButtonProps: {
        severity: 'secondary',
      },
      accept: () => {
        void this.deleteAccountAfterConfirm();
      },
    });
  }

  private async deleteAccountAfterConfirm() {
    if (this.submitting) return;

    this.submitting = true;

    try {
      await this.userService.deleteCurrentAccount();
      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.userSettingsPage.deleteAccountSuccess'),
        life: DEFAULT_SUCCESS_LIFE,
      });
      await this.authService.logout({
        sync: true,
        callHttpLogout: true,
        ignoreError: true,
      });
    } catch (err) {
      this.errorMessageService.handleError(err, this.messageService);
    } finally {
      this.submitting = false;
    }
  }
}
