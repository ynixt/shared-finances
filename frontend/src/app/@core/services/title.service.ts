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
    const translatedAppTitle = await this.getTranslatedAppTitle();

    if (newTitle) {
      try {
        const translatedTitle: string = await this.translocoService.selectTranslate(newTitle, translateArgs).pipe(take(1)).toPromise();
        this.title.setTitle(this.concatenateTitleAndAppTitle(translatedTitle, translatedAppTitle));

        return;
      } catch (_) {}
    }

    this.title.setTitle(translatedAppTitle);
  }

  public async getCurrentTitle(removeAppTitle = true): Promise<string> {
    if (removeAppTitle) {
      const translatedAppTitle = await this.getTranslatedAppTitle();
      return this.title.getTitle().replace(this.concatenateTitleAndAppTitle('', translatedAppTitle), '');
    }

    return this.title.getTitle();
  }

  private getTranslatedAppTitle(): Promise<string> {
    return this.translocoService.selectTranslate('app-title').pipe(take(1)).toPromise();
  }

  private concatenateTitleAndAppTitle(title: string, appTitle: string): string {
    return `${title} | ${appTitle}`;
  }
}
