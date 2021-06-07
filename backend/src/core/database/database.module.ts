import { Global, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import * as mongoose from 'mongoose';

mongoose.set('debug', true);
mongoose.set('useFindAndModify', false);

@Global()
@Module({
  imports: [MongooseModule.forRoot('mongodb://localhost/sharedFinances')],
  exports: [MongooseModule],
})
export class DatabaseModule {}
