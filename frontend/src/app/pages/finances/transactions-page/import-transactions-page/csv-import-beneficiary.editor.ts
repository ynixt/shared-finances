import { Injectable, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { validateBeneficiarySplit } from '../shared/transaction-form/transaction-form.beneficiaries';
import { UserForBeneficiary } from '../shared/transaction-form/transaction-form.types';
import { CsvImportCatalogStore } from './csv-import-catalog.store';
import { CsvImportRowResolver } from './csv-import-row.resolver';
import { normalizeHeader } from './csv-statement-parser';
import { ImportPreviewRow } from './import-transactions.models';

@Injectable()
export class CsvImportBeneficiaryEditor {
  private readonly catalogs = inject(CsvImportCatalogStore);
  private readonly rowResolver = inject(CsvImportRowResolver);
  private readonly formBuilder = inject(FormBuilder);

  visible = false;
  editingRow?: ImportPreviewRow;
  members: UserForBeneficiary[] = [];
  form = this.createForm();

  async open(row: ImportPreviewRow): Promise<void> {
    if (row.groupId == null || row.groupId === '') return;
    this.rowResolver.ensureDefaultBeneficiary(row);
    this.editingRow = row;
    this.members = await this.catalogs.ensureGroupMembers(row.groupId);
    this.rowResolver.ensureDefaultBeneficiary(row, this.members);
    const resolved = row.beneficiaries
      .map(leg => ({
        user: this.members.find(
          member => member.id === leg.userId || (leg.email != null && member.email.toLowerCase() === leg.email.toLowerCase()),
        ),
        benefitPercent: leg.benefitPercent,
      }))
      .filter((leg): leg is { user: UserForBeneficiary; benefitPercent: number } => leg.user != null);
    this.form = this.createForm(resolved);
    this.visible = true;
  }

  addLeg(): void {
    const array = this.form.get('extraBeneficiaryLegs') as FormArray;
    if (array.length === 0) {
      this.form.get('primaryBeneficiaryPercent')?.setValue(50);
      array.push(this.createLeg(undefined, 50));
    } else {
      array.push(this.createLeg());
    }
    this.form.updateValueAndValidity();
  }

  removeLeg(index: number): void {
    const array = this.form.get('extraBeneficiaryLegs') as FormArray;
    array.removeAt(index);
    if (array.length === 0) this.form.get('primaryBeneficiaryPercent')?.setValue(100);
    this.form.updateValueAndValidity();
  }

  async loadMembers(_page = 0, query?: string): Promise<UserForBeneficiary[]> {
    const normalized = normalizeHeader(query ?? '');
    return this.members.filter(member => normalized === '' || normalizeHeader(member.label).includes(normalized));
  }

  save(): void {
    this.form.markAllAsTouched();
    this.form.updateValueAndValidity();
    if (this.form.invalid || this.editingRow == null) return;
    const primaryUser = this.form.get('primaryBeneficiaryUser')?.value as UserForBeneficiary | undefined;
    const primaryPercent = Number(this.form.get('primaryBeneficiaryPercent')?.value);
    const extra = (this.form.get('extraBeneficiaryLegs') as FormArray).getRawValue() as Array<{
      benefitPercent: number;
      user: UserForBeneficiary;
    }>;
    this.editingRow.beneficiaries = [
      ...(primaryUser == null ? [] : [{ userId: primaryUser.id, benefitPercent: primaryPercent }]),
      ...extra.map(leg => ({ userId: leg.user.id, benefitPercent: Number(leg.benefitPercent) })),
    ];
    this.visible = false;
  }

  private createForm(legs: Array<{ user: UserForBeneficiary; benefitPercent: number }> = []): FormGroup {
    const [primary, ...extra] = legs;
    const form = this.formBuilder.group({
      primaryBeneficiaryUser: [primary?.user, Validators.required],
      primaryBeneficiaryPercent: [primary?.benefitPercent ?? 100, [Validators.required, Validators.min(0.01), Validators.max(100)]],
      extraBeneficiaryLegs: this.formBuilder.array(extra.map(leg => this.createLeg(leg.user, leg.benefitPercent))),
    });
    form.addValidators(group =>
      validateBeneficiarySplit({
        groupId: this.editingRow?.groupId,
        primaryBeneficiaryUser: group.get('primaryBeneficiaryUser')?.value,
        primaryBeneficiaryPercent: group.get('primaryBeneficiaryPercent')?.value,
        extraBeneficiaryLegs: (group.get('extraBeneficiaryLegs') as FormArray).getRawValue(),
      }),
    );
    return form;
  }

  private createLeg(user?: UserForBeneficiary, benefitPercent?: number): FormGroup {
    return this.formBuilder.group({
      user: [user, Validators.required],
      benefitPercent: [benefitPercent, [Validators.required, Validators.min(0.01), Validators.max(100)]],
    });
  }
}
