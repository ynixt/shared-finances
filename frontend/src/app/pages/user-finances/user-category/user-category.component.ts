import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { UserCategoryState } from 'src/app/store/reducers/user-category.reducer';
import { UserCategorySelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-user-category',
  templateUrl: './user-category.component.html',
  styleUrls: ['./user-category.component.scss'],
})
export class UserCategoryComponent implements OnInit {
  categoriesState$: Observable<UserCategoryState>;

  constructor(private userCategorySelectors: UserCategorySelectors) {}

  ngOnInit(): void {
    this.categoriesState$ = this.userCategorySelectors.state$;
  }
}
