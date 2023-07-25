import { Group } from './group';

export interface Category {
  id?: string;
  name: string;
  color: string;
  group?: Group;
  groupId?: number;
}
