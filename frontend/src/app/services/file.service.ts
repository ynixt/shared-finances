import { Injectable, inject } from '@angular/core';

import { PresignedService } from '../pages/finances/services/presigned.service';

@Injectable({ providedIn: 'root' })
export class FileService {
  presignedService = inject(PresignedService);

  async getRealUrl(url: string | null): Promise<string | null> {
    if (url == null) return null;

    if (url.startsWith('/private/external')) {
      try {
        return await this.presignedService.getUrl(url);
      } catch (err) {
        console.error(err);
        return null;
      }
    } else {
      return url;
    }
  }
}
