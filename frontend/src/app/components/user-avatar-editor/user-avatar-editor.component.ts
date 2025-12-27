import { Component, ElementRef, forwardRef, inject, input, viewChild } from '@angular/core';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { TranslatePipe } from '@ngx-translate/core';

import { md5 } from 'js-md5';
import { ImageCroppedEvent, ImageCropperComponent } from 'ngx-image-cropper';
import { Button, ButtonDirective } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Ripple } from 'primeng/ripple';

import { UserSimpleDto } from '../../models/generated/com/ynixt/sharedfinances/application/web/dto/user';
import { FileService } from '../../services/file.service';
import { SimpleControlValueAccessor } from '../simple-control-value-accessor';
import { UserAvatarComponent } from '../user-avatar/user-avatar.component';

@Component({
  selector: 'app-user-avatar-editor',
  imports: [UserAvatarComponent, ButtonDirective, Ripple, TranslatePipe, Dialog, Button, ImageCropperComponent],
  templateUrl: './user-avatar-editor.component.html',
  styleUrl: './user-avatar-editor.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UserAvatarEditorComponent),
      multi: true,
    },
  ],
})
export class UserAvatarEditorComponent extends SimpleControlValueAccessor<string | null> {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly fileService = inject(FileService);

  size = input<number>(256);
  user = input<UserSimpleDto | undefined>(undefined);
  inputFile = viewChild<ElementRef<HTMLInputElement> | undefined>('inputFile');
  dialogOpen = false;
  imageChangedEvent: Event | undefined = undefined;
  currentValueUrl: string | null = null;
  urlInCrop: string | null = null;

  delete() {
    this.onValueChange(null);
  }

  edit() {
    this.inputFile()?.nativeElement.click();
  }

  fromGravatar() {
    const user = this.user();

    if (!user) return;

    const hash = md5(user.email.trim().toLowerCase());
    const size = 128;
    const rating = 'g';

    this.onValueChange(`https://www.gravatar.com/avatar/${hash}?s=${size}&d=404}&r=${rating}`);
  }

  fileChangeEvent(event: Event) {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.imageChangedEvent = undefined;
      return;
    }

    this.imageChangedEvent = event;
    this.dialogOpen = true;
  }

  imageCropped(event: ImageCroppedEvent) {
    if (event.objectUrl) {
      // const croppedImage = this.sanitizer.bypassSecurityTrustUrl(event.objectUrl);
      this.urlInCrop = event.objectUrl;
    }
  }

  override writeValue(value: string | null | undefined) {
    super.writeValue(value);

    if (value == null) {
      this.currentValueUrl = null;
    } else {
      this.fileService.getRealUrl(value).then(value => (this.currentValueUrl = value));
    }
  }

  ok() {
    this.closeDialog();
    this.onValueChange(this.urlInCrop);
  }

  cancel() {
    this.closeDialog();
  }

  private closeDialog() {
    this.dialogOpen = false;
    this.imageChangedEvent = undefined;
    this.inputFile()!!.nativeElement.value = '';
  }
}
