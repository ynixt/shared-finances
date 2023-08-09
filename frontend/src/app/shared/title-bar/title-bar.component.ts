import { Component, Input, OnInit, ViewEncapsulation } from "@angular/core";

@Component({
  selector: 'app-title-bar',
  templateUrl: './title-bar.component.html',
  styleUrls: ['./title-bar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TitleBarComponent implements OnInit {
  @Input() title: string;

  constructor() {}

  ngOnInit(): void {}
}
