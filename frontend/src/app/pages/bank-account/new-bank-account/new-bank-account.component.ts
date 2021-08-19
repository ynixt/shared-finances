import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { BankAccount } from 'src/app/@core/models';
import { BankAccountService, ErrorService } from 'src/app/@core/services';
import { BankAccountDispatchers } from 'src/app/store';

@Component({
  selector: 'app-new-bank-account',
  templateUrl: './new-bank-account.component.html',
  styleUrls: ['./new-bank-account.component.scss'],
})
export class NewBankAccountComponent implements OnInit {
  formGroup: FormGroup;

  constructor(
    private bankAccountService: BankAccountService,
    private router: Router,
    private bankAccountDispatchers: BankAccountDispatchers,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
  ) {}

  ngOnInit(): void {
    this.formGroup = new FormGroup({
      name: new FormControl('', [Validators.required, Validators.maxLength(30)]),
      enabled: new FormControl(true, [Validators.required]),
      displayOnGroup: new FormControl(true, [Validators.required]),
    });
  }

  save(): void {
    if (this.formGroup.valid) {
      this.newBankAccount({
        name: this.formGroup.value.name,
        enabled: !!this.formGroup.value.enabled,
        displayOnGroup: !!this.formGroup.value.displayOnGroup,
      });
    }
  }

  private async newBankAccount(bankAccountInput: Partial<BankAccount>): Promise<void> {
    const bankAccountSaved = await this.bankAccountService
      .newBankAccount(bankAccountInput)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('creating'),
          success: this.translocoService.translate('creating-successful', { name: bankAccountInput.name }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'creating-error', 'creating-error-with-description', {
              name: bankAccountInput.name,
            }),
        }),
      )
      .toPromise();

    this.bankAccountDispatchers.bankAccountAdded(bankAccountSaved);
    this.router.navigateByUrl('/finances/bank');
  }
}
