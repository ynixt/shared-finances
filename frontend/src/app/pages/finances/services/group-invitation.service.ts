import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { lastValueFrom, take } from 'rxjs';

import { GroupInviteDto } from '../../../models/generated/com/ynixt/sharedfinances/application/web/dto/groups';
import { UserService } from '../../../services/user.service';
import { UserMissingError } from '../errors/user-missing.error';

@Injectable({
  providedIn: 'root',
})
export class GroupInvitationService {
  constructor(
    private http: HttpClient,
    private userService: UserService,
  ) {}
  async generateNewInvitation(groupId: string): Promise<GroupInviteDto> {
    const user = await this.userService.getUser();

    if (user != null) {
      return lastValueFrom(this.http.post<GroupInviteDto>(`/api/groups/${groupId}/members/generate-invitation`, undefined).pipe(take(1)));
    }

    throw new UserMissingError();
  }
}
