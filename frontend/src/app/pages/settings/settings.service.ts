import { Injectable } from "@angular/core";
import { UserSettings } from "../../@core/models";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

@Injectable()
export class SettingsService {

  constructor(private httpClient: HttpClient) {
  }

  updateSettings(newSettings: UserSettings): Observable<void> {
    return this.httpClient.post<void>("/api/user/settings", newSettings);
  }

  deleteUser(): Observable<void> {
    return this.httpClient.delete<void>("/api/user");
  }
}
