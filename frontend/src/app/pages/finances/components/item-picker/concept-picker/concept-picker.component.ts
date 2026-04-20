import { Component, effect, inject, input } from '@angular/core';
import { ControlContainer, FormGroupDirective, ReactiveFormsModule } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';

import { Select } from 'primeng/select';

import { CategoryConceptDto } from '../../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { CUSTOM_CATEGORY_CONCEPT_OPTION_ID, DEBT_SF_CONCEPT_CODE } from '../../../services/category-concept-form.util';
import { UserCategoriesService } from '../../../services/user-categories.service';

type ConceptPickerOption = {
  id: string;
  label: string;
};

@Component({
  selector: 'app-concept-picker',
  standalone: true,
  imports: [ReactiveFormsModule, Select],
  templateUrl: './concept-picker.component.html',
  styleUrl: './concept-picker.component.scss',
  viewProviders: [{ provide: ControlContainer, useExisting: FormGroupDirective }],
})
export class ConceptPickerComponent {
  private readonly formGroupDirective = inject(FormGroupDirective);
  private readonly userCategoriesService = inject(UserCategoriesService);
  private readonly translateService = inject(TranslateService);

  controlName = input.required<string>();
  excludeDebtSf = input(false);
  currentConceptId = input<string | null | undefined>(undefined);
  includeCustomOption = input(false);
  autoSelectFirst = input(true);

  options: ConceptPickerOption[] = [];

  constructor() {
    effect(() => {
      this.controlName();
      this.excludeDebtSf();
      this.currentConceptId();
      this.includeCustomOption();
      this.autoSelectFirst();

      void this.reloadOptions();
    });
  }

  private async reloadOptions(): Promise<void> {
    try {
      const concepts = await this.userCategoriesService.getAvailableConcepts();
      this.options = this.toOptions(concepts);
    } catch (error) {
      this.options = [];
      console.error('Failed to load concept options.', error);
    }
  }

  private toOptions(concepts: CategoryConceptDto[]): ConceptPickerOption[] {
    const currentConceptId = this.currentConceptId();
    const selectable = concepts
      .filter(concept => !this.excludeDebtSf() || concept.code !== DEBT_SF_CONCEPT_CODE || concept.id === currentConceptId)
      .map(concept => ({
        id: concept.id,
        label: concept.displayName ?? this.translateService.instant(`financesPage.categoriesPage.concepts.codes.${concept.code}`),
      }));

    if (this.includeCustomOption()) {
      selectable.push({
        id: CUSTOM_CATEGORY_CONCEPT_OPTION_ID,
        label: this.translateService.instant('financesPage.categoriesPage.concepts.customOption'),
      });
    }

    return selectable;
  }
}
