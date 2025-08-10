import { HttpClient } from '@angular/common/http';
import { Component, OnInit, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormControl, FormGroup, NG_VALUE_ACCESSOR, ReactiveFormsModule, Validators } from '@angular/forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TranslateService } from '@ngx-translate/core';

import { PrimeNG } from 'primeng/config';
import { Select } from 'primeng/select';

import { environment } from '../../../environments/environment';
import { i18nIsReady } from '../../util/i18n-util';
import { updatePrimeI18n } from '../../util/prime-i18n';

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
    private primengConfig: PrimeNG,
    private translateService: TranslateService,
    private httpClient: HttpClient,
  ) {}

  async ngOnInit() {
    this.loadOptions();

    this.formGroup.valueChanges.pipe(untilDestroyed(this)).subscribe(() => {
      const lang = this.formGroup.value.lang ?? this.defaultLang;
      this.translateService.use(lang);
    });

    this.translateService.onLangChange.pipe(untilDestroyed(this)).subscribe(e => {
      updatePrimeI18n(this.primengConfig, this.translateService, this.httpClient);
      this.loadOptions();
      this.value = lang;
      this.onChange(this.value);
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

  private loadOptions() {
    this.options = this.languages
      .map(langCode => ({
        value: langCode,
        name: this.translateService.instant('lang.' + langCode),
      }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }
}
