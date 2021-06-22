import { DragRef } from '@angular/cdk/drag-drop';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Injectable, Renderer2 } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { IDraggableRefs, ResizableDraggableDialog, TdDialogService } from '@covalent/core/dialogs';
import { Apollo, gql } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { Transaction } from 'src/app/@core/models';
import { NewTransactionComponentArgs } from './new-transaction-component-args';
import { NewTransactionComponent } from './new-transaction.component';

@Injectable({
  providedIn: 'root',
})
export class NewTransactionService {
  constructor(private dialogService: TdDialogService, private apollo: Apollo, private breakpointObserver: BreakpointObserver) {}

  openDialog(document: any, renderer2: Renderer2, shared: boolean): MatDialogRef<NewTransactionComponent, any> {
    const isMobile = this.breakpointObserver.isMatched([Breakpoints.Small, Breakpoints.XSmall]);

    const data: NewTransactionComponentArgs = {
      shared,
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

  newTransaction(transaction: Partial<Transaction>): Observable<Transaction> {
    return this.apollo
      .mutate<{ newTransacation: Transaction }>({
        mutation: gql`
          mutation(
            $transactionType: String!
            $date: String!
            $value: Float!
            $description: String
            $bankAccountId: String
            $bankAccount2Id: String
            $categoryId: String
            $creditCardId: String
            $groupId: String
            $firstUserId: String!
            $secondUserId: String
          ) {
            newTransacation(
              transactionType: $transactionType
              date: $date
              value: $value
              description: $description
              bankAccountId: $bankAccountId
              bankAccount2Id: $bankAccount2Id
              categoryId: $categoryId
              creditCardId: $creditCardId
              groupId: $groupId
              firstUserId: $firstUserId
              secondUserId: $secondUserId
            ) {
              id
              transactionType
              date
              value
              description
              category {
                id
                name
                color
              }
            }
          }
        `,
        variables: {
          transactionType: transaction.transactionType,
          date: transaction.date,
          value: transaction.value,
          description: transaction.description,
          bankAccountId: transaction.bankAccountId,
          bankAccount2Id: transaction.bankAccount2Id,
          categoryId: transaction.categoryId,
          creditCardId: transaction.creditCardId,
          groupId: transaction.groupId,
          firstUserId: transaction.user?.id,
          secondUserId: transaction.user2?.id,
        },
      })
      .pipe(
        take(1),
        map(result => result.data.newTransacation),
      );
  }
}
