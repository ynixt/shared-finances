import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { NewEntryDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/walletentry';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class WalletEntryService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
  ) {}

  async createWalletEntry(newEntryRequest: NewEntryDto): Promise<any> {
    const user = await this.userService.getUser();

    if (user != null) {
      await lastValueFrom(this.http.post<void>(`/api/wallet-entries`, newEntryRequest).pipe(take(1)));
      return;
    }

    throw new UserMissingError();
  }
}
