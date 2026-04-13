import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import {
  CreateSimulationJobRequestDto,
  SimulationJobDto,
} from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/simulationjobs';
import { Page, PageRequest } from '../../../models/pagination';
import { PaginationService } from '../../../services/pagination.service';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class SimulationJobService {
  constructor(
    private httpClient: HttpClient,
    private paginationService: PaginationService,
    private userService: UserService,
  ) {}

  private async ensureUserLoaded() {
    const user = await this.userService.getUser();
    if (user == null) throw new UserMissingError();
  }

  private basePath(groupId?: string): string {
    if (groupId != null && groupId !== '') {
      return `/api/groups/${groupId}/simulation-jobs`;
    }
    return '/api/simulation-jobs';
  }

  async createJob(body?: CreateSimulationJobRequestDto, groupId?: string): Promise<SimulationJobDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.post<SimulationJobDto>(this.basePath(groupId), body ?? {}).pipe(take(1)));
  }

  async listJobs(pageRequest?: PageRequest, groupId?: string): Promise<Page<SimulationJobDto>> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.paginationService.get<SimulationJobDto>(this.basePath(groupId), pageRequest).pipe(take(1)));
  }

  async getJob(jobId: string, groupId?: string): Promise<SimulationJobDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.get<SimulationJobDto>(`${this.basePath(groupId)}/${jobId}`).pipe(take(1)));
  }

  async cancelJob(jobId: string, groupId?: string): Promise<SimulationJobDto> {
    await this.ensureUserLoaded();
    return lastValueFrom(this.httpClient.post<SimulationJobDto>(`${this.basePath(groupId)}/${jobId}/cancel`, {}).pipe(take(1)));
  }

  async deleteJob(jobId: string, groupId?: string): Promise<void> {
    await this.ensureUserLoaded();
    await lastValueFrom(this.httpClient.delete<void>(`${this.basePath(groupId)}/${jobId}`).pipe(take(1)));
  }
}
