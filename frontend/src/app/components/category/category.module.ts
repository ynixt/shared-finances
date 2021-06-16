import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { CurrencyMaskModule } from 'ng2-currency-mask';
import { MatInputModule } from '@angular/material/input';

import { CategoryComponent } from './category.component';
import { SharedModule } from 'src/app/shared/shared.module';
import { MatMenuModule } from '@angular/material/menu';
import { CovalentDialogsModule } from '@covalent/core/dialogs';
import { FormCategoryComponent } from './form-category/form-category.component';
import { EditCategoryComponent } from './edit-category/edit-category.component';
import { NewCategoryComponent } from './new-category/new-category.component';
import { RouterModule } from '@angular/router';

const components = [CategoryComponent, NewCategoryComponent, FormCategoryComponent, EditCategoryComponent];

@NgModule({
  declarations: [...components],
  imports: [
    CommonModule,
    RouterModule,
    SharedModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    ReactiveFormsModule,
    CurrencyMaskModule,
    MatMenuModule,
    CovalentDialogsModule,
  ],
  exports: [...components],
})
export class CategoryModule {}
