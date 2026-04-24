import { Component, inject, input, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { UserResponseDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { VersionService } from '../../services/version.service';

@Component({
  selector: 'app-footer',
  imports: [TranslatePipe],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
})
export class FooterComponent {
  private readonly versionService = inject(VersionService);

  user = input<UserResponseDto | undefined | null>(undefined);

  backendVersion = signal<string>('???');
  frontendVersion = signal<string>(this.versionService.frontendVersion);

  constructor() {
    this.versionService.getBackendVersion().then(version => this.backendVersion.set(version));
  }
}
