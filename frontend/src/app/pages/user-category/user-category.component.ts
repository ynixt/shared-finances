import { Component, OnInit } from '@angular/core';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { Category } from 'src/app/@core/models';
import { UserCategorySelectors } from 'src/app/store/services/selectors';

@UntilDestroy()
@Component({
  selector: 'app-user-category',
  templateUrl: './user-category.component.html',
  styleUrls: ['./user-category.component.scss'],
})
export class UserCategoryComponent implements OnInit {
  categories: Category[];

  constructor(private userCategorySelectors: UserCategorySelectors) {}

  ngOnInit(): void {
    this.userCategorySelectors.categories$.pipe(untilDestroyed(this)).subscribe(categories => (this.categories = categories));
  }
}
