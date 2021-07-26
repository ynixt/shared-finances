import { Global, Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import mongoose from 'mongoose';

mongoose.set('debug', process.env.NODE_ENV !== 'production');
mongoose.set('useFindAndModify', false);

@Global()
@Module({
  imports: [MongooseModule.forRoot(process.env.DB_URL ?? 'mongodb://localhost/sharedFinances')],
  exports: [MongooseModule],
})
export class DatabaseModule {}
