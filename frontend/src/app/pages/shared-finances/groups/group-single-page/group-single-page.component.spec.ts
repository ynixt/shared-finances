import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupSinglePageComponent } from './group-single-page.component';

describe('GroupSinglePageComponent', () => {
  let component: GroupSinglePageComponent;
  let fixture: ComponentFixture<GroupSinglePageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [GroupSinglePageComponent],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupSinglePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
