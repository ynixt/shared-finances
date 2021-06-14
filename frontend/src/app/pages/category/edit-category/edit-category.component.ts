import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { Category } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { CategoryService } from '../category.service';

@Component({
  selector: 'app-edit-category',
  templateUrl: './edit-category.component.html',
  styleUrls: ['./edit-category.component.scss'],
})
export class EditCategoryComponent implements OnInit {
  category: Category;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private categoryService: CategoryService,
    private errorService: ErrorService,
    private toast: HotToastService,
    private translocoService: TranslocoService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.params.subscribe(params => this.getCategory(params.id));
  }

  async edit(categoryInput: Category): Promise<void> {
    await this.categoryService
      .editCategory(categoryInput)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('editing'),
          success: this.translocoService.translate('editing-successful', { name: categoryInput.name }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'editing-error', 'editing-error-with-description', {
              name: categoryInput.name,
            }),
        }),
      )
      .toPromise();

    this.router.navigateByUrl('/category');
  }

  private async getCategory(categoryId: string) {
    try {
      const category = await this.categoryService.getById(categoryId);

      if (!category) {
        this.router.navigateByUrl('/404');
      }

      this.category = category;
    } catch (err) {
      this.toast.error(this.errorService.getInstantErrorMessage(err, 'generic-error', 'generic-error-with-description'));
    }
  }
}
