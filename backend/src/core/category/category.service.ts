import { Injectable } from '@nestjs/common';
import { AuthenticationError } from 'apollo-server-express';
import { GroupService } from '../group';
import { Category } from '../models';
import { EditCategoryArgs, NewCategoryArgs, NewGroupCategoryArgs } from '../models/args';
import { CategoryRepository } from './category.repository';

@Injectable()
export class CategoryService {
  constructor(private categoryRepository: CategoryRepository, private groupService: GroupService) {}

  create(userId: string, newCategory: NewCategoryArgs): Promise<Category> {
    return this.categoryRepository.create({ ...newCategory, userId });
  }

  async createFromGroup(loggedUserId: string, newCategory: NewGroupCategoryArgs): Promise<Category> {
    if (!(await this.groupService.userHasAccessToGroup(loggedUserId, newCategory.groupId))) {
      throw new AuthenticationError('');
    }

    return this.categoryRepository.create({ ...newCategory, groupId: newCategory.groupId });
  }

  update(userId: string, newCategory: EditCategoryArgs): Promise<Category> {
    return this.categoryRepository.update({ ...newCategory, userId });
  }

  async updateFromGroup(loggedUserId: string, newCategory: EditCategoryArgs): Promise<Category> {
    const category = await this.getWithoutCheckPermission(newCategory.id);

    if (!(await this.groupService.userHasAccessToGroup(loggedUserId, category.groupId))) {
      throw new AuthenticationError('');
    }

    return this.categoryRepository.update({ ...newCategory, groupId: category.groupId });
  }

  findAllWithUserId(userId: string): Promise<Category[]> {
    return this.categoryRepository.findAllWithUserId(userId);
  }

  async findAllWithGroupId(loggedUserId: string, groupId: string): Promise<Category[]> {
    if (!(await this.groupService.userHasAccessToGroup(loggedUserId, groupId))) {
      throw new AuthenticationError('');
    }

    return this.categoryRepository.findAllWithGroupId(groupId);
  }

  async getById(options: { loggedUserId: string; categoryId: string; groupId: string }): Promise<Category | null> {
    if (options.groupId == null) {
      return this.categoryRepository.findByUserId(options.loggedUserId, options.categoryId);
    } else {
      if (!(await this.groupService.userHasAccessToGroup(options.loggedUserId, options.groupId))) {
        throw new AuthenticationError('');
      }

      return this.categoryRepository.findByGroupId(options.groupId, options.categoryId);
    }
  }

  delete(userId: string, categoryId: string): Promise<Category | null> {
    return this.categoryRepository.delete(userId, categoryId);
  }

  async deleteFromGroup(loggedUserId: string, categoryId: string): Promise<Category | null> {
    const category = await this.getWithoutCheckPermission(categoryId);

    if (!(await this.groupService.userHasAccessToGroup(loggedUserId, category.groupId))) {
      throw new AuthenticationError('');
    }

    return this.categoryRepository.deleteFromGroup(category.groupId, categoryId);
  }

  public getWithoutCheckPermission(categoryId: string): Promise<Category | null> {
    return this.categoryRepository.getById(categoryId);
  }
}
