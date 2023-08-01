import { Injectable } from "@angular/core";
import { take } from "rxjs/operators";
import { HttpClient } from "@angular/common/http";
import { lastValueFrom } from "rxjs";

@Injectable({
  providedIn: "root"
})
export class InviteService {
  constructor(private httpClient: HttpClient) {
  }

  async useInvite(invite: string): Promise<string> {
    return lastValueFrom(this.httpClient.get<string>(`/api/group/invite/${invite}`).pipe(take(1)));
  }
}
