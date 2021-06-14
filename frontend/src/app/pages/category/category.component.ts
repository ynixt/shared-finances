import invert from 'invert-color';
import { Component, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { Category } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { UserCategorySelectors } from 'src/app/store/services/selectors';
import { UserCategoryService } from 'src/app/@core/services';

@UntilDestroy()
@Component({
  selector: 'app-category',
  templateUrl: './category.component.html',
  styleUrls: ['./category.component.scss'],
})
export class CategoryComponent implements OnInit {
  categories: Category[] = [];

  constructor(
    private userCategoryService: UserCategoryService,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
    private userCategorySelectors: UserCategorySelectors,
  ) {}

  async ngOnInit(): Promise<void> {
    this.userCategorySelectors.categories$.pipe(untilDestroyed(this)).subscribe(categories => this.fillCategories(categories));
  }

  async delete(category: Category): Promise<void> {
    const confirm = await this.dialogService
      .openConfirm({
        title: this.translocoService.translate('confirm'),
        message: this.translocoService.translate('delete-confirm', { name: category.name }),
        cancelButton: this.translocoService.translate('cancel'),
        acceptButton: this.translocoService.translate('delete'),
        width: '500px',
      })
      .afterClosed()
      .pipe(take(1))
      .toPromise();
    if (confirm) {
      this.toast.observe;
      await this.userCategoryService
        .deleteCategory(category.id)
        .pipe(
          take(1),
          this.toast.observe({
            loading: this.translocoService.translate('deleting'),
            success: this.translocoService.translate('deleting-successful', { name: category.name }),
            error: error =>
              this.errorService.getInstantErrorMessage(error, 'deleting-error', 'deleting-error-with-description', {
                name: category.name,
              }),
          }),
        )
        .toPromise();
    }
  }

  getFontColor(category: Category): string {
    return invert(category.color, true);
  }

  private fillCategories(categories: Category[]): void {
    this.categories = [...(categories || [])].sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name));
  }
}
