import {field, ID, objectType} from '@loopback/graphql';
import {Entity, model, property} from '@loopback/repository';

@objectType()
@model()
export class User extends Entity {
  @field(type => ID)
  @property({id: true, type: 'string'})
  id?: string;

  @field()
  @property({
    index: true
  })
  uid: string;
}
