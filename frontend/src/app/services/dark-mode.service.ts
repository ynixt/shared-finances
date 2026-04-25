import { Injectable, signal } from '@angular/core';

import { UserResponseDto } from '../models/generated/com/ynixt/sharedfinances/application/web/dto/user';

const DARK_MODE_STORAGE_KEY = 'darkMode';

@Injectable({ providedIn: 'root' })
export class DarkModeService {
  private readonly darkModeState = signal(false);
  readonly darkMode = this.darkModeState.asReadonly();

  initialize() {
    const stored = localStorage.getItem(DARK_MODE_STORAGE_KEY);
    this.applyDarkMode(stored === 'true', false);
  }

  isDarkMode(): boolean {
    return this.darkModeState();
  }

  toggleDarkMode(): boolean {
    const newValue = !this.darkModeState();
    this.applyDarkMode(newValue);
    return newValue;
  }

  setDarkMode(enabled: boolean) {
    this.applyDarkMode(enabled);
  }

  syncFromUser(user: UserResponseDto | null) {
    if (user == null) {
      return;
    }

    this.applyDarkMode(user.darkMode);
  }

  private applyDarkMode(enabled: boolean, persist = true) {
    this.darkModeState.set(enabled);

    if (persist) {
      localStorage.setItem(DARK_MODE_STORAGE_KEY, enabled ? 'true' : 'false');
    }

    const element = document.querySelector('html');
    element?.classList.toggle('dark-mode', enabled);
  }
}
