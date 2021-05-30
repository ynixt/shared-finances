import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthDispatchers } from 'src/app/store';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements OnInit, OnDestroy {
  private isLoggedSubscription: Subscription;

  constructor(private authDispatchers: AuthDispatchers, private router: Router, private authSelectors: AuthSelectors) {}

  ngOnInit(): void {
    this.isLoggedSubscription = this.authSelectors.isLogged$.subscribe(isLogged => {
      if (isLogged) {
        this.router.navigateByUrl('/finances');
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
