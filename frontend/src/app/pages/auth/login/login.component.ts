import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { take } from 'rxjs/operators';
import { AuthDispatchers } from 'src/app/store';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit, OnDestroy {
  private isLoggedSubscription: Subscription;

  constructor(
    private authDispatchers: AuthDispatchers,
    private router: Router,
    private authSelectors: AuthSelectors,
    private activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.isLoggedSubscription = this.authSelectors.isLogged$.subscribe(async isLogged => {
      if (isLogged) {
        const queryParams = await this.activatedRoute.queryParams.pipe(take(1)).toPromise();
        const url = queryParams.next || '/finances';

        this.router.navigateByUrl(url);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.isLoggedSubscription) {
      this.isLoggedSubscription.unsubscribe();
    }
  }

  public loginByGoogle(): void {
    this.authDispatchers.loginByGoogle();
  }
}
