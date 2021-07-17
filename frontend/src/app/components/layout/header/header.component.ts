import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { AuthDispatchers } from 'src/app/store';
import { AuthState } from 'src/app/store/reducers/auth.reducer';
import { AuthSelectors } from 'src/app/store/services/selectors';
import { BreakpointObserver, Breakpoints, BreakpointState } from '@angular/cdk/layout';
import { Location } from '@angular/common';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit, OnDestroy {
  userState$: Observable<AuthState>;
  userOpen: boolean;

  isMobile = false;

  private observerSmallSubscription: Subscription;
  private observerMediumSubscription: Subscription;

  get isHome() {
    return ['', '/'].includes(this.location.path());
  }

  constructor(
    private authSelectors: AuthSelectors,
    private authDispatchers: AuthDispatchers,
    private router: Router,
    private breakpointObserver: BreakpointObserver,
    private location: Location,
  ) {}

  ngOnInit(): void {
    this.userState$ = this.authSelectors.state$;

    console.log(this.location.path());

    this.observerSmallSubscription = this.breakpointObserver
      .observe([Breakpoints.Small, Breakpoints.XSmall])
      .subscribe((state: BreakpointState) => {
        this.isMobile = state.matches;
      });

    this.observerMediumSubscription = this.breakpointObserver
      .observe([Breakpoints.Medium, Breakpoints.Large, Breakpoints.XLarge])
      .subscribe((state: BreakpointState) => {
        this.isMobile = !state.matches;
      });
  }

  ngOnDestroy(): void {
    if (this.observerSmallSubscription) {
      this.observerSmallSubscription.unsubscribe();
    }
    if (this.observerMediumSubscription) {
      this.observerMediumSubscription.unsubscribe();
    }
  }

  async logout(): Promise<void> {
    await this.authDispatchers.logout();
    this.login();
  }

  login(): void {
    this.router.navigateByUrl('/auth/login');
  }

  getToolbarLink(userIsLogged: boolean): string {
    return userIsLogged ? '/finances' : '/';
  }

  shouldShowMenu(userIsLogged: boolean) {
    return this.isHome === false && userIsLogged;
  }
}
