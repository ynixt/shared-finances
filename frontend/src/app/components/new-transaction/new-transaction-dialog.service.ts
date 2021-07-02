import { DragRef } from '@angular/cdk/drag-drop';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Injectable, Renderer2 } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { IDraggableRefs, ResizableDraggableDialog, TdDialogService } from '@covalent/core/dialogs';
import { Transaction } from 'src/app/@core/models';
import { NewTransactionComponentArgs } from './new-transaction-component-args';
import { NewTransactionComponent } from './new-transaction.component';

@Injectable({
  providedIn: 'root',
})
export class NewTransactionDialogService {
  constructor(private dialogService: TdDialogService, private breakpointObserver: BreakpointObserver) {}

  openDialog(
    document: any,
    renderer2: Renderer2,
    shared: boolean,
    transactionForEdit?: Transaction,
  ): MatDialogRef<NewTransactionComponent, any> {
    const isMobile = this.breakpointObserver.isMatched([Breakpoints.Small, Breakpoints.XSmall]);

    const data: NewTransactionComponentArgs = {
      shared,
      transaction: transactionForEdit,
    };

    const { matDialogRef, dragRefSubject }: IDraggableRefs<NewTransactionComponent> = this.dialogService.openDraggable({
      component: NewTransactionComponent,
      dragHandleSelectors: ['mat-toolbar'],
      config: {
        panelClass: ['td-window-dialog'],
        data,
      },
    });

    matDialogRef.componentInstance.closed.subscribe(() => matDialogRef.close());

    if (isMobile === false) {
      let resizableDraggableDialog: ResizableDraggableDialog;
      dragRefSubject.subscribe((dragRf: DragRef) => {
        resizableDraggableDialog = new ResizableDraggableDialog(document, renderer2, matDialogRef, dragRf);
      });

      matDialogRef.afterClosed().subscribe(() => resizableDraggableDialog.detach());
    }

    return matDialogRef;
  }
}
