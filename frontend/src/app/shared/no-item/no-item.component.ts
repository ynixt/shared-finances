import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-no-item',
  templateUrl: './no-item.component.html',
  styleUrls: ['./no-item.component.scss'],
})
export class NoItemComponent implements OnInit {
  @Input() text: string;
  @Input() buttonText: string;
  @Input() buttonRouterLink: string;
  @Input() buttonClick: () => void;

  constructor() {}

  ngOnInit(): void {}
}
