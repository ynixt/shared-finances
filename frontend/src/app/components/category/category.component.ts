import invert from 'invert-color';
import { Component, Input, OnInit } from '@angular/core';
import { TdDialogService } from '@covalent/core/dialogs';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { Category } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { GenericCategoryService } from './generic-category.service';

@Component({
  selector: 'app-category',
  templateUrl: './category.component.html',
  styleUrls: ['./category.component.scss'],
})
export class CategoryComponent implements OnInit {
  private _categories: Category[] = [];

  @Input() loading = false;

  @Input() set categories(categories: Category[]) {
    this._categories = [...(categories || [])].sort((creditCardA, creditCardB) => creditCardA.name.localeCompare(creditCardB.name));
  }

  get categories() {
    return this._categories;
  }

  constructor(
    private categoryService: GenericCategoryService,
    private dialogService: TdDialogService,
    private translocoService: TranslocoService,
    private toast: HotToastService,
    private errorService: ErrorService,
  ) {}

  ngOnInit(): void {}

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
      await this.categoryService
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
}
