import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Group } from 'src/app/@core/models/group';

@Component({
  selector: 'app-form-group',
  templateUrl: './form-group.component.html',
  styleUrls: ['./form-group.component.scss'],
})
export class FormGroupComponent implements OnInit {
  formGroup: FormGroup;

  @Input() group: Group;

  @Output() groupSaved = new EventEmitter<Group>();

  get isNew() {
    return this.group == null;
  }

  constructor() {}

  ngOnInit(): void {
    this.formGroup = new FormGroup({ name: new FormControl(this.group?.name, [Validators.required, Validators.maxLength(30)]) });
  }

  save(): void {
    if (this.formGroup.valid) {
      this.groupSaved.emit({
        id: this.group?.id,
        name: this.formGroup.value.name,
      });
    }
  }
}
