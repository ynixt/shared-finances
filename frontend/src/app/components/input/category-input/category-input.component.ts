import { Component, forwardRef, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { ControlContainer, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { BehaviorSubject, combineLatest, Observable, Subscription } from 'rxjs';
import { map, startWith } from 'rxjs/operators';

import { ControlValueAccessorConnector } from 'src/app/@core/control-value-accessor-connector';
import { Category } from 'src/app/@core/models';
import { Group } from 'src/app/@core/models/group';
import { SharedCategoryService } from 'src/app/pages/shared-finances/shared-category/shared-category.service';
import { UserCategorySelectors } from 'src/app/store/services/selectors';

@UntilDestroy()
@Component({
  selector: 'app-category-input',
  templateUrl: './category-input.component.html',
  styleUrls: ['./category-input.component.scss'],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => CategoryInputComponent), multi: true }],
})
export class CategoryInputComponent extends ControlValueAccessorConnector<Category[]> implements OnInit, ControlValueAccessor, OnChanges {
  filteredCategories$: Observable<Category[]>;

  @Input() group: Group;
  @Input() isShared: boolean;

  private categoriesSubject: BehaviorSubject<Category[]> = new BehaviorSubject([]);
  private categoriesSourceSubscription: Subscription;

  constructor(
    controlContainer: ControlContainer,
    private userCategorySelectors: UserCategorySelectors,
    private sharedCategoryService: SharedCategoryService,
  ) {
    super(controlContainer);
  }

  ngOnInit(): void {
    this.mountFilteredCategories();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.categoriesSourceSubscription?.unsubscribe();

    if (this.isShared) {
      if (changes.group?.currentValue) {
        this.categoriesSourceSubscription = this.sharedCategoryService
          .watchCategories(changes.group.currentValue.id)
          .pipe(untilDestroyed(this))
          .subscribe(categories => {
            this.categoriesSubject.next(categories);
          });
      } else {
        this.categoriesSubject.next([]);
      }
    } else {
      this.categoriesSourceSubscription = this.userCategorySelectors.categories$.pipe(untilDestroyed(this)).subscribe(categories => {
        this.categoriesSubject.next(categories);
      });
    }
  }

  formatCategory(category: Category): string {
    return category?.name;
  }

  private mountFilteredCategories(): void {
    this.filteredCategories$ = combineLatest([this.control.valueChanges.pipe(startWith('')), this.categoriesSubject]).pipe(
      map(combined => ({ categories: combined[1], query: combined[0] })),
      map(combined => {
        return combined.categories != null
          ? combined.categories
              .filter(category => {
                const query = typeof combined.query === 'string' ? combined.query : combined.query.name;

                return category.name.toLowerCase().includes(query.toLowerCase());
              })
              .sort((categoryA, categoryB) => categoryA.name.localeCompare(categoryB.name))
          : [];
      }),
    );
  }
}
