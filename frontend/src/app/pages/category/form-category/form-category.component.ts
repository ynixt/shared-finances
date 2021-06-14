import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Category } from 'src/app/@core/models';

@Component({
  selector: 'app-form-category',
  templateUrl: './form-category.component.html',
  styleUrls: ['./form-category.component.scss'],
})
export class FormCategoryComponent implements OnInit {
  @Input() category: Category;
  @Output() categorySaved = new EventEmitter<Category>();

  get isNew() {
    return this.category == null;
  }

  formGroup: FormGroup;

  constructor() {}

  ngOnInit(): void {
    this.formGroup = new FormGroup({
      name: new FormControl(this.category?.name, [Validators.required, Validators.maxLength(30)]),
      color: new FormControl(this.category?.color ?? this.randomColor(), [
        Validators.required,
        Validators.maxLength(7),
        Validators.maxLength(7),
      ]),
    });
  }

  save(): void {
    if (this.formGroup.valid) {
      this.categorySaved.emit({
        id: this.category?.id,
        name: this.formGroup.value.name,
        color: this.formGroup.value.color,
      });
    }
  }

  private randomColor(): string {
    return `#${(0x1000000 + Math.random() * 0xffffff).toString(16).substr(1, 6)}`;
  }
}
