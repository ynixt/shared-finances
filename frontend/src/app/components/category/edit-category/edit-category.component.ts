import { Component, Inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { Category } from 'src/app/@core/models';
import { UserCategoryService, ErrorService } from 'src/app/@core/services';
import { GENERIC_CATEGORY_URL_TOKEN } from '..';
import { GenericCategoryService } from '../generic-category.service';

@Component({
  selector: 'app-edit-category',
  templateUrl: './edit-category.component.html',
  styleUrls: ['./edit-category.component.scss'],
})
export class EditCategoryComponent implements OnInit {
  category: Category;

  get individualBreadcrumbUrl() {
    const urls = this.categoryUrl.split('/').slice(0, 3);
    urls[0] = '/' + urls[0];
    return urls;
  }

  get categoriesBreadcrumbUrl() {
    return this.categoryUrl;
  }

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private categoryService: GenericCategoryService,
    private errorService: ErrorService,
    private toast: HotToastService,
    private translocoService: TranslocoService,
    @Inject(GENERIC_CATEGORY_URL_TOKEN) private categoryUrl: string,
  ) {}

  async ngOnInit(): Promise<void> {
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

    this.router.navigateByUrl(this.categoryUrl);
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
