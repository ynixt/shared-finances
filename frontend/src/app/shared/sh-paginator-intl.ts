import { MatPaginatorIntl } from "@angular/material/paginator";
import { Injectable } from "@angular/core";
import { TranslocoService } from "@ngneat/transloco";
import { BehaviorSubject, Subject } from "rxjs";

@Injectable()
export class ShPaginatorIntl implements MatPaginatorIntl {
  readonly changes: Subject<void> = new BehaviorSubject(null);

  firstPageLabel: string;
  lastPageLabel: string;
  nextPageLabel: string;
  previousPageLabel: string;
  itemsPerPageLabel: string;

  constructor(private translocoService: TranslocoService) {
    this.loadTexts();
  }

  private async loadTexts() {
    this.firstPageLabel = await this.translocoService.translate("pagination.firstPage");
    this.lastPageLabel = await this.translocoService.translate("pagination.lastPage");
    this.nextPageLabel = await this.translocoService.translate("pagination.nextPage");
    this.previousPageLabel = await this.translocoService.translate("pagination.previousPage");
    this.itemsPerPageLabel = await this.translocoService.translate("pagination.itemsPerPage");
    this.changes.next();
  }


  getRangeLabel(page: number, pageSize: number, length: number): string {
    const amountPages = Math.ceil(length / pageSize);

    return this.translocoService.translate("pagination.pageRange", {
      currentPage: page + 1,
      totalPages: amountPages
    });
  }
}
