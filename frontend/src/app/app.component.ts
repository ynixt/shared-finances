import { Component, OnInit } from '@angular/core';
import { AuthDispatchers } from './store';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent implements OnInit {
  title = 'shared-finances';

  constructor(private authDispatchers: AuthDispatchers) {}

  ngOnInit(): void {
    this.authDispatchers.getCurrentUser();
  }
}
