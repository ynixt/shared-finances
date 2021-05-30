import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { User } from 'src/app/@core/models';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  user$: Observable<User>;

  constructor(private authSelectors: AuthSelectors) {}

  ngOnInit(): void {
    this.user$ = this.authSelectors.user$;
  }
}
