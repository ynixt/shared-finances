import { Injectable } from '@nestjs/common';
import { Category } from '../models';
import { EditCategoryArgs, NewCategoryArgs } from '../models/args';
import { CategoryRepository } from './category.repository';

@Injectable()
export class CategoryService {
  constructor(private categoryRepository: CategoryRepository) {}

  create(userId: string, newCategory: NewCategoryArgs): Promise<Category> {
    return this.categoryRepository.create({ ...newCategory, userId });
  }

  update(userId: string, newCategory: EditCategoryArgs): Promise<Category> {
    return this.categoryRepository.update({ ...newCategory, userId });
  }

  findAllWithUserId(userId: string): Promise<Category[]> {
    return this.categoryRepository.findAllWithUserId(userId);
  }

  getById(userId: string, categoryId: string): Promise<Category | null> {
    return this.categoryRepository.findByUserId(userId, categoryId);
  }

  delete(userId: string, categoryId: string): Promise<Category | null> {
    return this.categoryRepository.delete(userId, categoryId);
  }
}
