import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { CreateExportDto, ExportBatchDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/exports';

export interface ExportDownloadMetadata {
  firstDownloadedAt: string | null;
  downloadExpiresAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class ExportService {
  private readonly http = inject(HttpClient);

  create(request: CreateExportDto): Promise<ExportBatchDto> {
    return lastValueFrom(this.http.post<ExportBatchDto>('/api/exports', request).pipe(take(1)));
  }

  list(): Promise<ExportBatchDto[]> {
    return lastValueFrom(this.http.get<ExportBatchDto[]>('/api/exports').pipe(take(1)));
  }

  async download(batch: ExportBatchDto): Promise<ExportDownloadMetadata> {
    const response = await lastValueFrom(
      this.http.get(`/api/exports/${batch.id}/download`, { observe: 'response', responseType: 'blob' }).pipe(take(1)),
    );
    const blob = response.body;
    if (blob == null) throw new Error('Export download returned an empty response');
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `transactions-${batch.id}.${batch.format.toLowerCase()}`;
    anchor.click();
    URL.revokeObjectURL(url);
    return {
      firstDownloadedAt: response.headers.get('X-Export-First-Downloaded-At'),
      downloadExpiresAt: response.headers.get('X-Export-Download-Expires-At'),
    };
  }

  delete(id: string): Promise<void> {
    return lastValueFrom(this.http.delete<void>(`/api/exports/${id}`).pipe(take(1)));
  }
}
