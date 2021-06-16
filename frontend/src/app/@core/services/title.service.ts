import { Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { HashMap, TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class TitleService {
  constructor(private title: Title, private translocoService: TranslocoService) {}

  public async changeTitle(newTitle: string, translateArgs?: HashMap): Promise<void> {
    const translatedAppTitle: string = await this.translocoService.selectTranslate('app-title').pipe(take(1)).toPromise();

    if (newTitle) {
      try {
        const translatedTitle: string = await this.translocoService.selectTranslate(newTitle, translateArgs).pipe(take(1)).toPromise();
        this.title.setTitle(`${translatedTitle} | ${translatedAppTitle}`);

        return;
      } catch (_) {}
    }

    this.title.setTitle(translatedAppTitle);
  }
}
