import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthState } from 'src/app/store/reducers/auth.reducer';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-not-found',
  templateUrl: './not-found.component.html',
  styleUrls: ['./not-found.component.scss'],
})
export class NotFoundComponent implements OnInit {
  authState$: Observable<AuthState>;

  constructor(private authSelectors: AuthSelectors) {}

  ngOnInit(): void {
    this.authState$ = this.authSelectors.state$;
  }
}
