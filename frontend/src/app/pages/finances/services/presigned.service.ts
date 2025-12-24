import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({ providedIn: 'root' })
export class PresignedService {
  constructor(
    private userService: UserService,
    private http: HttpClient,
  ) {}

  async getUrl(path: string): Promise<string> {
    const user = await this.userService.getUser();

    if (user != null) {
      const wrapper = await lastValueFrom(this.http.get<{ value: string }>(path).pipe(take(1)));
      return wrapper.value;
    }

    throw new UserMissingError();
  }
}
