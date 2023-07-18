import { forwardRef, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { GroupModule } from '../group';
import { Category, CategorySchema } from '../models';
import { CategoryRepository } from './category.repository';
import { CategoryResolver } from './category.resolver';
import { CategoryService } from './category.service';

@Module({
  imports: [MongooseModule.forFeature([{ name: Category.name, schema: CategorySchema }]), forwardRef(() => GroupModule)],
  providers: [CategoryRepository, CategoryService, CategoryResolver],
  exports: [CategoryService],
})
export class CategoryModule {}
