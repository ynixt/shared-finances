import { Component, effect, input, output } from '@angular/core';

import { Aria } from 'primeng/api';
import { ButtonDirective } from 'primeng/button';
import { PrimeNG } from 'primeng/config';
import { Ripple } from 'primeng/ripple';

@Component({
  selector: 'app-infinite-paginator',
  imports: [Ripple, ButtonDirective],
  templateUrl: './infinite-paginator.component.html',
  styleUrl: './infinite-paginator.component.scss',
  standalone: true,
})
export class InfinitePaginatorComponent {
  hasNext = input<boolean>(false);
  initialPage = input<number>(0);
  pageChange = output<number>();

  currentPage = 0;

  constructor(private config: PrimeNG) {
    effect(() => {
      this.currentPage = this.initialPage();
    });
  }

  nextPage() {
    this.changePage(this.currentPage + 1);
  }

  previousPage() {
    this.changePage(this.currentPage - 1);
  }

  firstPage() {
    this.changePage(0);
  }

  getAriaLabel(labelType: keyof Aria): string | undefined {
    if (this.config.translation?.aria == null) return undefined;
    return this.config.translation.aria[labelType];
  }

  private changePage(newPage: number) {
    this.currentPage = newPage;
    this.pageChange.emit(newPage);
  }
}
