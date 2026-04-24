import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { environment } from '../../environments/environment';
import { VersionResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto';

@Injectable({
  providedIn: 'root',
})
export class VersionService {
  public readonly frontendVersion = environment.version;
  private readonly httpClient = inject(HttpClient);

  private backendVersion: string | undefined;

  async getBackendVersion(): Promise<string> {
    let version: string | undefined = this.backendVersion;

    if (version != null) {
      return version;
    }

    version = (await lastValueFrom(this.httpClient.get<VersionResponseDto>('/api/open/version').pipe(take(1)))).version;

    this.backendVersion = version;

    return version;
  }
}
