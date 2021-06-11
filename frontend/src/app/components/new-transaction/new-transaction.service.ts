import { DragRef } from '@angular/cdk/drag-drop';
import { Injectable, Renderer2 } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { IDraggableRefs, ResizableDraggableDialog, TdDialogService } from '@covalent/core/dialogs';
import { NewTransactionComponent } from './new-transaction.component';

@Injectable({
  providedIn: 'root',
})
export class NewTransactionService {
  constructor(private dialogService: TdDialogService) {}

  openDialog(document: any, renderer2: Renderer2): MatDialogRef<NewTransactionComponent, any> {
    const { matDialogRef, dragRefSubject }: IDraggableRefs<NewTransactionComponent> = this.dialogService.openDraggable({
      component: NewTransactionComponent,
      dragHandleSelectors: ['mat-toolbar'],
      config: {
        panelClass: ['td-window-dialog'], // pass this class in to ensure certain css is properly added
      },
    });

    matDialogRef.componentInstance.closed.subscribe(() => matDialogRef.close());

    let resizableDraggableDialog: ResizableDraggableDialog;
    dragRefSubject.subscribe((dragRf: DragRef) => {
      resizableDraggableDialog = new ResizableDraggableDialog(document, renderer2, matDialogRef, dragRf);
    });

    // Detach resize-ability event listeners after dialog closes
    matDialogRef.afterClosed().subscribe(() => resizableDraggableDialog.detach());

    return matDialogRef;
  }
}
