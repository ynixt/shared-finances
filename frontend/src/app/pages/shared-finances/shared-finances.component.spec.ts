import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedFinancesComponent } from './shared-finances.component';

describe('SharedFinancesComponent', () => {
  let component: SharedFinancesComponent;
  let fixture: ComponentFixture<SharedFinancesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SharedFinancesComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SharedFinancesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
