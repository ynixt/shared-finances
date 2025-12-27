import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { Avatar } from 'primeng/avatar';
import { Skeleton } from 'primeng/skeleton';
import { Tooltip } from 'primeng/tooltip';

import { UserSimpleDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { FileService } from '../../services/file.service';

export type UserAvatarSize = 'normal' | 'large' | 'xlarge';

export const convertUserAvatarSizeToRem: (size: UserAvatarSize) => number = (size: UserAvatarSize) => {
  switch (size) {
    case 'normal':
      return 2;
    case 'large':
      return 3;
    case 'xlarge':
      return 4;
    default:
      return 0;
  }
};

@Component({
  selector: 'app-user-avatar',
  imports: [Avatar, TranslatePipe, Tooltip, Skeleton],
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.scss',
})
export class UserAvatarComponent {
  fileService = inject(FileService);

  tooltipPosition = input<'bottom' | 'left' | 'top' | 'right'>('bottom');
  user = input<UserSimpleDto | undefined>(undefined);
  customImageUrl = input<string | null | undefined>(undefined);
  showTooltip = input<boolean>(true);
  size = input<UserAvatarSize>('normal');

  label = computed<string | undefined>(() => {
    const user = this.user();

    if (user == null) return undefined;

    return user.firstName[0].toUpperCase() + user.lastName[0].toUpperCase();
  });

  sizeInRem = computed(() => {
    return convertUserAvatarSizeToRem(this.size());
  });

  imageUrl = signal<string | undefined>(undefined);

  constructor() {
    effect(() => {
      const user = this.user();
      const customImageUrl = this.customImageUrl();

      if (customImageUrl === undefined) {
        if (user == null) return undefined;
        if (user.photoUrl == null) return this.imageUrl.set(undefined);
        else {
          this.fileService.getRealUrl(user.photoUrl).then(url => this.imageUrl.set(url == null ? undefined : url));
        }
      } else {
        this.imageUrl.set(customImageUrl == null ? undefined : customImageUrl);
      }
    });
  }
}
