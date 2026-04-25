import { Component, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { VersionService } from '../../services/version.service';

@Component({
  selector: 'app-footer',
  imports: [TranslatePipe],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  private readonly versionService = inject(VersionService);

  backendVersion = signal<string>('???');
  frontendVersion = signal<string>(this.versionService.frontendVersion);

  constructor() {
    this.versionService.getBackendVersion().then(version => this.backendVersion.set(version));
  }
}
