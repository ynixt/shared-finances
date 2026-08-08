import { Component, input } from '@angular/core';
import { FormArray, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { ButtonDirective } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';

import { PagedSelectComponent } from '../../../../../components/paged-select/paged-select.component';
import { RequiredFieldAsteriskComponent } from '../../../../../components/required-field-asterisk/required-field-asterisk.component';
import { BeneficiaryLegForm, UserForBeneficiary } from '../transaction-form/transaction-form.types';

@Component({
  selector: 'app-beneficiary-split',
  imports: [ReactiveFormsModule, TranslatePipe, ButtonDirective, InputNumber, Select, PagedSelectComponent, RequiredFieldAsteriskComponent],
  templateUrl: './beneficiary-split.component.html',
})
export class BeneficiarySplitComponent {
  readonly form = input.required<FormGroup>();
  readonly membersGetter = input.required<(page: number, query: string | undefined) => Promise<UserForBeneficiary[]>>();
  readonly members = input<UserForBeneficiary[]>();
  readonly showClear = input(true);
  readonly addLeg = input.required<() => void>();
  readonly removeLeg = input.required<(index: number) => void>();

  get extraBeneficiaryLegs(): FormArray<BeneficiaryLegForm> {
    return this.form().get('extraBeneficiaryLegs') as FormArray<BeneficiaryLegForm>;
  }
}
