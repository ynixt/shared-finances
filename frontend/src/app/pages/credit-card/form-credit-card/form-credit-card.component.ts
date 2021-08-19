import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { CreditCard } from 'src/app/@core/models';

@Component({
  selector: 'app-form-credit-card',
  templateUrl: './form-credit-card.component.html',
  styleUrls: ['./form-credit-card.component.scss'],
})
export class FormCreditCardComponent implements OnInit {
  @Input() creditCard: CreditCard;
  @Output() creditCardSaved = new EventEmitter<CreditCard>();

  get isNew() {
    return this.creditCard == null;
  }

  formGroup: FormGroup;

  constructor() {}

  ngOnInit(): void {
    this.formGroup = new FormGroup({
      name: new FormControl(this.creditCard?.name, [Validators.required, Validators.maxLength(30)]),
      limit: new FormControl(this.creditCard?.limit, [Validators.required, Validators.min(0), Validators.max(1000000)]),
      closingDay: new FormControl(this.creditCard ? this.creditCard.closingDay : 1, [Validators.required]),
      paymentDay: new FormControl(this.creditCard ? this.creditCard.paymentDay : 1, [Validators.required]),
      enabled: new FormControl(this.creditCard?.enabled ?? true),
      displayOnGroup: new FormControl(this.creditCard?.displayOnGroup ?? true),
    });
  }

  save(): void {
    if (this.formGroup.valid) {
      this.creditCardSaved.emit({
        id: this.creditCard?.id,
        name: this.formGroup.value.name,
        limit: this.formGroup.value.limit,
        closingDay: this.formGroup.value.closingDay,
        paymentDay: this.formGroup.value.paymentDay,
        enabled: !!this.formGroup.value.enabled,
        displayOnGroup: !!this.formGroup.value.displayOnGroup,
      });
    }
  }
}
