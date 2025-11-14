import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { InputText } from 'primeng/inputtext';

import { RequiredFieldAsteriskComponent } from '../../../../components/required-field-asterisk/required-field-asterisk.component';
import { DEFAULT_ERROR_LIFE } from '../../../../util/error-util';
import { DEFAULT_SUCCESS_LIFE } from '../../../../util/success-util';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-new-group-page',
  imports: [
    FinancesTitleBarComponent,
    TranslatePipe,
    ButtonDirective,
    FormsModule,
    InputText,
    ReactiveFormsModule,
    RequiredFieldAsteriskComponent,
  ],
  templateUrl: './new-group-page.component.html',
  styleUrl: './new-group-page.component.scss',
})
export class NewGroupPageComponent {
  readonly formGroup: FormGroup;

  submitting = false;

  constructor(
    fb: FormBuilder,
    private groupService: GroupService,
    private messageService: MessageService,
    private translateService: TranslateService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.formGroup = fb.group({
      name: ['', [Validators.required]],
    });
  }

  async submit() {
    if (this.formGroup.invalid || this.submitting) {
      return;
    }

    this.submitting = true;

    try {
      const group = await this.groupService.newGroup(this.formGroup.value);

      this.messageService.add({
        severity: 'success',
        summary: this.translateService.instant('general.success'),
        detail: this.translateService.instant('financesPage.groupsPage.newGroupPage.successMessage'),
        life: DEFAULT_SUCCESS_LIFE,
      });

      await this.router.navigate(['..', group.id], { relativeTo: this.route });
    } catch (err) {
      this.submitting = false;

      console.error(err);

      this.messageService.add({
        severity: 'error',
        summary: this.translateService.instant('error.genericTitle'),
        detail: this.translateService.instant('error.genericMessage'),
        life: DEFAULT_ERROR_LIFE,
      });
    }
  }
}
