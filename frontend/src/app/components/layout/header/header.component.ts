import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthDispatchers } from 'src/app/store';
import { AuthState } from 'src/app/store/reducers/auth.reducer';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
})
export class HeaderComponent implements OnInit {
  userState$: Observable<AuthState>;
  userOpen: boolean;

  constructor(private authSelectors: AuthSelectors, private authDispatchers: AuthDispatchers, private router: Router) {}

  ngOnInit(): void {
    this.userState$ = this.authSelectors.state$;
  }

  async logout(): Promise<void> {
    this.authDispatchers.logout();
  }

  async login(): Promise<void> {
    this.router.navigateByUrl('/auth/login');
  }
}
