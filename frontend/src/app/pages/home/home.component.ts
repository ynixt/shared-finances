import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthSelectors } from 'src/app/store/services/selectors';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  private authSubscription: Subscription;

  constructor(private authSelectors: AuthSelectors, private router: Router) {}

  ngOnInit(): void {
    this.authSubscription = this.authSelectors.state$.subscribe(authState => {
      if (authState.done && authState.user) {
        this.router.navigateByUrl('/finances');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }
}
