import { Global, Module } from '@nestjs/common';
import { ErrorUtilService } from './error-util.service';

@Global()
@Module({
  providers: [ErrorUtilService],
  exports: [ErrorUtilService],
})
export class SharedModule {}
