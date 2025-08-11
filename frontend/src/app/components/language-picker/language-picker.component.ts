import { Component, OnInit, effect, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule, Validators } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { Select } from 'primeng/select';

import { environment } from '../../../environments/environment';
import { LangService } from '../../services/lang.service';
import { i18nIsReady } from '../../util/i18n-util';

@Component({
  selector: 'app-language-picker',
  imports: [ReactiveFormsModule, Select],
  templateUrl: './language-picker.component.html',
  styleUrl: './language-picker.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LanguagePickerComponent),
      multi: true,
    },
  ],
})
@UntilDestroy()
export class LanguagePickerComponent implements ControlValueAccessor, OnInit {
  readonly defaultLang = 'en-US';
  readonly languages = ['pt-BR', 'en-US'];
  readonly formGroup = new FormGroup({
    lang: new FormControl(this.defaultLang, [Validators.required]),
  });

  options: { name: string; value: string }[] = [];

  value: any = this.defaultLang;

  private onChange: (value: any) => void = () => {};
  private onTouched: () => void = () => {};

  constructor(
    private translateService: TranslateService,
    private langService: LangService,
  ) {
    effect(() => {
      const lang = this.langService.currentLang();

      this.writeValue(lang);
      this.onChange(lang);
    });
  }

  async ngOnInit() {
    this.options = await this.langService.getAllLanguages();

    this.formGroup.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      const lang = this.formGroup.value.lang ?? this.defaultLang;
      this.langService.changeLanguage(lang, true);
    });

    await i18nIsReady(this.translateService);
    const lang = this.translateService.currentLang ?? environment.defaultLanguage;

    this.formGroup.get('lang')!.setValue(lang);
  }

  writeValue(value: any): void {
    this.value = value;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }
}
