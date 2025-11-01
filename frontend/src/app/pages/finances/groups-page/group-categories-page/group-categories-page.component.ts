import { Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faArrowUpRightFromSquare, faPenToSquare } from '@fortawesome/free-solid-svg-icons';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslatePipe } from '@ngx-translate/core';

import { TreeNode } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TreeTableLazyLoadEvent, TreeTableModule } from 'primeng/treetable';

import { CategoryDto } from '../../../../models/generated/com/ynixt/sharedfinances/application/web/dto/wallet/category';
import { Page } from '../../../../models/pagination';
import { createEmptyPage } from '../../../../services/pagination.service';
import { FinancesTitleBarComponent } from '../../components/finances-title-bar/finances-title-bar.component';
import { GroupCategoriesService } from '../../services/group-categories.service';

@UntilDestroy()
@Component({
  selector: 'app-group-user-categories-page',
  imports: [FinancesTitleBarComponent, TranslatePipe, ButtonDirective, FaIconComponent, RouterLink, Tooltip, TreeTableModule],
  templateUrl: './group-categories-page.component.html',
  styleUrl: './group-categories-page.component.scss',
})
export class GroupCategoriesPageComponent {
  readonly pageSize = 12;

  loading = true;
  categories: Page<CategoryDto> = createEmptyPage();
  categoriesTree: TreeNode[] = [];

  private currentPage = 0;
  private groupId: string | undefined;
  private skipNextLazyLoad = false;

  constructor(
    private route: ActivatedRoute,
    private groupCategoriesService: GroupCategoriesService,
  ) {
    this.route.paramMap.pipe(untilDestroyed(this)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.groupId = id;
        // Avoid double request on first render: we are about to load explicitly
        this.skipNextLazyLoad = true;
        this.loadPage(0);
      }
    });
  }

  onLazyLoad(event: TreeTableLazyLoadEvent) {
    if (this.skipNextLazyLoad) {
      this.skipNextLazyLoad = false;
      return;
    }

    const rows = (event.rows as number) ?? this.pageSize;
    const first = (event.first as number) ?? 0;
    const newPage = Math.floor(first / rows);
    this.loadPage(newPage);
  }

  private async loadPage(page = 0) {
    if (!this.groupId) return;

    this.loading = true;
    this.currentPage = page;

    this.categories = await this.groupCategoriesService.getAllCategories(this.groupId, undefined, {
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
