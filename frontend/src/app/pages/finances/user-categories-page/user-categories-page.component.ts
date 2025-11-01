import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faPenToSquare } from '@fortawesome/free-solid-svg-icons';
import { TranslatePipe } from '@ngx-translate/core';

import { TreeNode } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TreeTableLazyLoadEvent, TreeTableModule } from 'primeng/treetable';

import { CategoryDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page } from '../../../models/pagination';
import { createEmptyPage } from '../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../components/finances-title-bar/finances-title-bar.component';
import { UserCategoriesService } from '../services/user-categories.service';

@Component({
  selector: 'app-user-categories-page',
  imports: [FinancesTitleBarComponent, TranslatePipe, ButtonDirective, FaIconComponent, RouterLink, Tooltip, TreeTableModule],
  templateUrl: './user-categories-page.component.html',
  styleUrl: './user-categories-page.component.scss',
})
export class UserCategoriesPageComponent {
  readonly pageSize = 12;

  loading = true;
  categories: Page<CategoryDto> = createEmptyPage();
  categoriesTree: TreeNode[] = [];

  private currentPage = 0;

  constructor(private categoriesService: UserCategoriesService) {}

  onLazyLoad(event: TreeTableLazyLoadEvent) {
    const newPage = this.categories.totalPages == 0 ? 0 : Math.floor(event.first / this.categories.numberOfElements);
    this.loadPage(newPage);
  }

  private async loadPage(page = 0) {
    this.loading = true;
    this.currentPage = page;

    this.categories = await this.categoriesService.getAllCategories(undefined, {
      page,
      size: this.pageSize,
      sort: [
        {
          property: 'name',
          direction: 'ASC',
        },
      ],
    });

    this.categoriesTree = this.categories.content.map(c => ({
      label: c.name,
      data: c,
      children: c.children?.map(cc => ({ label: cc.name, data: cc })),
    }));

    this.loading = false;
  }

  protected readonly openFinanceIcon = faArrowUpRightFromSquare;
  protected readonly editFinanceIcon = faPenToSquare;
}
