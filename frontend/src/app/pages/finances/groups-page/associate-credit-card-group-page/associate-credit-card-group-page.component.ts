import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';
import { Select } from 'primeng/select';

import { GroupWithRoleDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { CreditCardForGroupAssociateDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/creditCard';
import { ErrorMessageService } from '../../../../services/error-message.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupAssociationService } from '../../services/group-association.service';
import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-associate-credit-card-group-page',
  imports: [FinancesTitleBarComponent, ProgressSpinner, TranslatePipe, ReactiveFormsModule, Button, Select],
  templateUrl: './associate-credit-card-group-page.component.html',
  styleUrl: './associate-credit-card-group-page.component.scss',
})
@UntilDestroy()
export class AssociateCreditCardGroupPageComponent {
  group: GroupWithRoleDto | null = null;
  loading = true;
  submitting = false;
  creditCards: CreditCardForGroupAssociateDto[] = [];
  formGroup: FormGroup;

  private groupId: string | undefined = undefined;

  constructor(
    private route: ActivatedRoute,
    private groupService: GroupService,
    private router: Router,
    private groupAssociationService: GroupAssociationService,
    private errorMessageService: ErrorMessageService,
    private messageService: MessageService,
    fb: FormBuilder,
  ) {
    this.formGroup = fb.group({
      creditCard: [undefined, [Validators.required]],
    });

    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');

      if (id) {
        this.getGroup(id);
      } else {
        this.goToNotFound();
      }
    });
  }

  async submit() {
    if (this.groupId == null || this.submitting || this.formGroup.invalid) return;

    this.submitting = true;

    try {
      await this.groupAssociationService.associateCreditCard(this.groupId, this.formGroup.value.creditCard);
      await this.router.navigate(['..'], { relativeTo: this.route });
    } catch (error) {
      this.errorMessageService.handleError(error, this.messageService);
      this.submitting = false;
      throw error;
    }
  }

  private async getGroup(id: string): Promise<void> {
    this.loading = true;
    this.groupId = id;

    try {
      this.group = await this.groupService.getGroup(id);
      await this.getAllowedCreditCards();

      this.loading = false;
    } catch (error) {
      if (error instanceof HttpErrorResponse) {
        if (error.status === 404 || error.status === 400) {
          await this.goToNotFound();
          return;
        }
      }

      throw error;
    }
  }

  private async getAllowedCreditCards() {
    if (this.group == null) return;

    this.loading = true;
    this.creditCards = await this.groupAssociationService.findAllAllowedCreditCardsToAssociate(this.group.id);
    this.loading = false;
  }

  private goToNotFound() {
    return this.router.navigateByUrl('/not-found');
  }
}
