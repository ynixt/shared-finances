import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-credit-card-bill-payment',
  templateUrl: './credit-card-bill-payment.component.html',
  styleUrls: ['./credit-card-bill-payment.component.scss'],
})
export class CreditCardBillPaymentComponent implements OnInit {
  @Output() closed: EventEmitter<void> = new EventEmitter();

  formGroup: FormGroup;

  constructor() {}

  ngOnInit(): void {}

  save(): void {}
}
