import { Component, Inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';
import { Category } from 'src/app/@core/models';
import { ErrorService } from 'src/app/@core/services/error.service';
import { GenericCategoryService } from '../generic-category.service';
import { GENERIC_CATEGORY_URL_TOKEN } from '..';
import { Group } from 'src/app/@core/models/group';

@Component({
  selector: 'app-new-category',
  templateUrl: './new-category.component.html',
  styleUrls: ['./new-category.component.scss'],
})
export class NewCategoryComponent implements OnInit {
  group?: Group;
  groupDone = false;

  get individualSharedBreadcrumbUrl() {
    const urls = this.categoryUrl.split('/').slice(0, 3);
    urls[0] = '/' + urls[0];
    return urls;
  }

  get categoriesBreadcrumbUrl() {
    return this.categoryUrl;
  }

  constructor(
    private categoryService: GenericCategoryService,
    private router: Router,
    private toast: HotToastService,
    private translocoService: TranslocoService,
    private errorService: ErrorService,
    @Inject(GENERIC_CATEGORY_URL_TOKEN) private categoryUrl: string,
    private activatedRoute: ActivatedRoute,
  ) {}

  async ngOnInit(): Promise<void> {
    this.groupDone = false;

    const { groupId } = await this.activatedRoute.params.pipe(take(1)).toPromise();

    if (groupId) {
      this.group = await this.categoryService.getGroup(groupId);
    }

    this.groupDone = true;
  }

  async newCategory(categoryInput: Category): Promise<void> {
    await this.categoryService
      .newCategory(categoryInput, this.group?.id)
      .pipe(
        take(1),
        this.toast.observe({
          loading: this.translocoService.translate('creating'),
          success: this.translocoService.translate('creating-successful', { name: categoryInput.name }),
          error: error =>
            this.errorService.getInstantErrorMessage(error, 'creating-error', 'creating-error-with-description', {
              name: categoryInput.name,
            }),
        }),
      )
      .toPromise();

    this.router.navigateByUrl(this.categoryUrl.replace(':groupId', this.group?.id));
  }
}
