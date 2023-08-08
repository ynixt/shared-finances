import { Component, forwardRef, Input, OnChanges, SimpleChanges } from "@angular/core";
import { ControlContainer, ControlValueAccessor, NG_VALUE_ACCESSOR } from "@angular/forms";
import { UntilDestroy, untilDestroyed } from "@ngneat/until-destroy";
import { Subscription } from "rxjs";

import { ControlValueAccessorConnector } from "src/app/@core/control-value-accessor-connector";
import { Category } from "src/app/@core/models";
import { Group } from "src/app/@core/models/group";
import { SharedCategoryService } from "src/app/pages/shared-finances/shared-category/shared-category.service";
import { UserCategorySelectors } from "src/app/store/services/selectors";

@UntilDestroy()
@Component({
  selector: "app-category-input",
  templateUrl: "./category-input.component.html",
  styleUrls: ["./category-input.component.scss"],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => CategoryInputComponent), multi: true }]
})
export class CategoryInputComponent extends ControlValueAccessorConnector<Category[]> implements ControlValueAccessor, OnChanges {
  categories: Category[] = [];

  @Input() group: Group;
  @Input() isShared: boolean;

  private categoriesSourceSubscription: Subscription;

  constructor(
    controlContainer: ControlContainer,
    private userCategorySelectors: UserCategorySelectors,
    private sharedCategoryService: SharedCategoryService
  ) {
    super(controlContainer);
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.categoriesSourceSubscription?.unsubscribe();

    if (this.isShared) {
      if (changes.group?.currentValue) {
        this.categoriesSourceSubscription = this.sharedCategoryService
          .watchCategories(changes.group.currentValue.id)
          .pipe(untilDestroyed(this))
          .subscribe(categories => {
            this.categories = categories.sort((categoryA, categoryB) => categoryA.name.localeCompare(categoryB.name));
          });
      } else {
        this.categories = [];
      }
    } else {
      this.categoriesSourceSubscription = this.userCategorySelectors.categories$.pipe(untilDestroyed(this)).subscribe(categories => {
        this.categories = categories != null ? [...categories].sort((categoryA, categoryB) => categoryA.name.localeCompare(categoryB.name)) : [];
      });
    }
  }

  categoryInputValueCompare(obj1: any, obj2: any): boolean {
    return obj1 === obj2 || (obj1 && obj2 && obj1.id === obj2.id);
  }
}
