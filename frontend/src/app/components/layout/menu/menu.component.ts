import { DOCUMENT } from "@angular/common";
import { Component, EventEmitter, Inject, OnInit, Output, Renderer2 } from "@angular/core";
import { NewTransactionDialogService } from "../../new-transaction/new-transaction-dialog.service";

@Component({
  selector: "app-menu",
  templateUrl: "./menu.component.html",
  styleUrls: ["./menu.component.scss"]
})
export class MenuComponent implements OnInit {
  @Output() onMenuClose = new EventEmitter<void>;

  constructor(
    private newTransactionService: NewTransactionDialogService,
    @Inject(DOCUMENT) private document: any,
    private renderer2: Renderer2
  ) {
  }

  ngOnInit(): void {
  }

  openNewTransactionDialog(shared = false) {
    this.newTransactionService.openDialog(this.document, this.renderer2, shared);
    this.closeMenu(false);
  }

  closeMenu(blurActiveElement = true) {
    this.onMenuClose.next();
    if (blurActiveElement && document.activeElement && document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  }
}
