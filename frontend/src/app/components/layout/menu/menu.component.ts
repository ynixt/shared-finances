import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit, Renderer2 } from '@angular/core';
import { NewTransactionDialogService } from '../../new-transaction/new-transaction-dialog.service';

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.scss'],
})
export class MenuComponent implements OnInit {
  constructor(
    private newTransactionService: NewTransactionDialogService,
    @Inject(DOCUMENT) private document: any,
    private renderer2: Renderer2,
  ) {}

  ngOnInit(): void {}

  openNewTransactionDialog(shared = false) {
    this.newTransactionService.openDialog(this.document, this.renderer2, shared);
  }
}
