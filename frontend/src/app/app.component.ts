import { Component, OnInit } from '@angular/core';
import { AuthDispatchers } from './store';
import { AuthSelectors } from './store/services/selectors';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  title = 'shared-finances';
  appDone: boolean;

  constructor(private authDispatchers: AuthDispatchers, private authSelectors: AuthSelectors) {}

  ngOnInit(): void {
    this.authDispatchers.getCurrentUser();
    this.authSelectors.done().then(done => (this.appDone = done));
  }
}
