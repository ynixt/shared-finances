import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  CreateImportDto,
  ImportBatchDto,
  ImportDuplicateCheckDto,
  ImportHashCheckDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/imports';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class ImportService {
  constructor(
    private readonly http: HttpClient,
    private readonly userService: UserService,
  ) {}

  async checkHash(hash: string): Promise<ImportHashCheckDto> {
    await this.requireUser();
    return lastValueFrom(this.http.get<ImportHashCheckDto>(`/api/imports/check-hash/${encodeURIComponent(hash)}`).pipe(take(1)));
  }

  async checkDuplicates(request: ImportDuplicateCheckDto): Promise<number[]> {
    await this.requireUser();
    return lastValueFrom(this.http.post<number[]>('/api/imports/check-duplicates', request).pipe(take(1)));
  }

  async create(request: CreateImportDto): Promise<ImportBatchDto> {
    await this.requireUser();
    return lastValueFrom(this.http.post<ImportBatchDto>('/api/imports', request).pipe(take(1)));
  }

  async list(): Promise<ImportBatchDto[]> {
    await this.requireUser();
    return lastValueFrom(this.http.get<ImportBatchDto[]>('/api/imports').pipe(take(1)));
  }

  async get(id: string): Promise<ImportBatchDto> {
    await this.requireUser();
    return lastValueFrom(this.http.get<ImportBatchDto>(`/api/imports/${id}`).pipe(take(1)));
  }

  async undo(id: string): Promise<ImportBatchDto> {
    await this.requireUser();
    return lastValueFrom(this.http.delete<ImportBatchDto>(`/api/imports/${id}`).pipe(take(1)));
  }

  private async requireUser(): Promise<void> {
    if ((await this.userService.getUser()) == null) {
      throw new UserMissingError();
    }
  }
}
