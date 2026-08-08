import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FileService {
  private readonly http = inject(HttpClient);

  async getRealUrl(url: string | null): Promise<string | null> {
    if (url == null) return null;

    if (url.startsWith('/api/private/avatars/')) {
      try {
        const blob = await lastValueFrom(this.http.get(url, { responseType: 'blob' }).pipe(take(1)));
        return URL.createObjectURL(blob);
      } catch (err) {
        console.error(err);
        return null;
      }
    }

    return /^(blob:|data:|https?:\/\/)/.test(url) ? url : null;
  }

  revokeObjectUrl(url: string | null | undefined): void {
    if (url?.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }
  }
}
