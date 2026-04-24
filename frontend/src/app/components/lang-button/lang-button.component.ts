import { Component, Input, OnInit } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { Button } from 'primeng/button';
import { Menu } from 'primeng/menu';

import { LangService } from '../../services/lang.service';

@Component({
  selector: 'app-lang-button',
  imports: [Menu, Button],
  templateUrl: './lang-button.component.html',
  styleUrl: './lang-button.component.scss',
})
export class LangButtonComponent implements OnInit {
  @Input() rounded: boolean = true;
  @Input() text: boolean = true;
  @Input() buttonClass: string | undefined;

  items: MenuItem[] = [];

  constructor(private langService: LangService) {}

  async ngOnInit() {
    this.items = (await this.langService.getAllLanguages()).map(ln => ({
      id: ln.value,
      label: ln.name,
      disabled: ln.current,
      command: () => {
        this.langService.changeLanguage(ln.value, true);
        this.items = this.items.map(i => ({ ...i, disabled: ln.value === i.id }));
      },
    }));
  }
}
