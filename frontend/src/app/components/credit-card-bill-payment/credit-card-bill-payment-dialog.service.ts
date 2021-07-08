import { DragRef } from '@angular/cdk/drag-drop';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Injectable, Renderer2 } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { IDraggableRefs, ResizableDraggableDialog, TdDialogService } from '@covalent/core/dialogs';
import { Transaction } from 'src/app/@core/models';
import { CreditCardBillPaymentComponentArgs } from './credit-card-bill-payment-component-args';
import { CreditCardBillPaymentComponent } from './credit-card-bill-payment.component';

@Injectable({
  providedIn: 'root',
})
export class CreditCardBillPaymentDialogService {
  constructor(private dialogService: TdDialogService, private breakpointObserver: BreakpointObserver) {}

  openDialog(
    document: any,
    renderer2: Renderer2,
    transactionForEdit?: Transaction,
  ): MatDialogRef<CreditCardBillPaymentComponent, any> {
    const isMobile = this.breakpointObserver.isMatched([Breakpoints.Small, Breakpoints.XSmall]);

    const data: CreditCardBillPaymentComponentArgs = {
      transaction: transactionForEdit,
    };

    const { matDialogRef, dragRefSubject }: IDraggableRefs<CreditCardBillPaymentComponent> = this.dialogService.openDraggable({
      component: CreditCardBillPaymentComponent,
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
