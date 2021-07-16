import { Component, OnInit } from '@angular/core';
import { AuthDispatchers } from 'src/app/store';

@Component({
  selector: 'app-google-button',
  templateUrl: './google-button.component.html',
  styleUrls: ['./google-button.component.scss'],
})
export class GoogleButtonComponent implements OnInit {
  constructor(private authDispatchers: AuthDispatchers) {}

  ngOnInit(): void {}

  public loginByGoogle(): void {
    this.authDispatchers.loginByGoogle();
  }
}
