import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormCreditCardComponent } from './form-credit-card.component';

describe('FormCreditCardComponent', () => {
  let component: FormCreditCardComponent;
  let fixture: ComponentFixture<FormCreditCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [FormCreditCardComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FormCreditCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
