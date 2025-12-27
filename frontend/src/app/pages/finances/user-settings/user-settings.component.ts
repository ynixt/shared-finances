import { Component, effect, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Panel } from 'primeng/panel';
import { Password } from 'primeng/password';
import { ProgressSpinner } from 'primeng/progressspinner';
import { TabsModule } from 'primeng/tabs';

import { CurrencySelectorComponent } from '../../../components/currency-selector/currency-selector.component';
import { LanguagePickerComponent } from '../../../components/language-picker/language-picker.component';
import { TimeZoneSelectorComponent } from '../../../components/timezone-selector/time-zone-selector.component';
import { UserAvatarEditorComponent } from '../../../components/user-avatar-editor/user-avatar-editor.component';
import { UserResponseDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
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
  ],
  templateUrl: './user-settings.component.html',
  styleUrl: './user-settings.component.scss',
})
export class UserSettingsComponent {
  private readonly userService = inject(UserService);
  private readonly errorMessageService = inject(ErrorMessageService);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);

  user: UserResponseDto | null = null;
  submitting = false;

  generalForm = new FormGroup({
    email: new FormControl<string | undefined>(undefined, [Validators.required]),
    name: new FormGroup({
      first: new FormControl<string | undefined>(undefined, [Validators.required, Validators.minLength(2)]),
      last: new FormControl<string | undefined>(undefined, [Validators.required, Validators.minLength(2)]),
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

  securityTwoStepForm = new FormGroup({
    currentPassword: new FormControl<string | undefined>(undefined),
  });

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

      this.securityPasswordForm.reset();

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
        currentPasswordHash: this.securityPasswordForm.get('currentPassword')!!.value!!,
        newPasswordHash: this.securityPasswordForm.get('newPassword')!!.value!!,
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
}
