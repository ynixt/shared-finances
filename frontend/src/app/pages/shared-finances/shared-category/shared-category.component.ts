import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { take } from 'rxjs/operators';
import { GenericCategoryService, GroupWithIdName } from 'src/app/components/category';
import { UserCategoryState, initialState as InitialCategoryState } from 'src/app/store/reducers/user-category.reducer';

@UntilDestroy()
@Component({
  selector: 'app-shared-category',
  templateUrl: './shared-category.component.html',
  styleUrls: ['./shared-category.component.scss'],
})
export class SharedCategoryComponent implements OnInit {
  categoriesState$: Observable<UserCategoryState>;
  group: GroupWithIdName;

  private categoriesStateSubject: BehaviorSubject<UserCategoryState>;

  constructor(private genericCategoryService: GenericCategoryService, private activatedRoute: ActivatedRoute, private router: Router) {
    this.categoriesStateSubject = new BehaviorSubject<UserCategoryState>({ ...InitialCategoryState, loading: true });
    this.categoriesState$ = this.categoriesStateSubject.asObservable();
  }

  async ngOnInit(): Promise<void> {
    const { groupId } = await this.activatedRoute.params.pipe(take(1)).toPromise();
    this.group = await this.genericCategoryService.getGroup(groupId);

    if (this.group == null) {
      this.router.navigateByUrl('/404');
    } else {
      this.genericCategoryService
        .watchCategories(this.group.id)
        .pipe(untilDestroyed(this))
        .subscribe(categories =>
          this.categoriesStateSubject.next({
            done: true,
            loading: false,
            categories,
          }),
        );
    }
  }
}
