import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { UserCategoryState } from 'src/app/store/reducers/user-category.reducer';
import { UserCategorySelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-shared-category',
  templateUrl: './shared-category.component.html',
  styleUrls: ['./shared-category.component.scss'],
})
export class SharedCategoryComponent implements OnInit {
  categoriesState$: Observable<UserCategoryState>;

  constructor(private userCategorySelectors: UserCategorySelectors) {}

  ngOnInit(): void {
    this.categoriesState$ = this.userCategorySelectors.state$;
  }
}
