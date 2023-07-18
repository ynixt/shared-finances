import { Global, Module } from '@nestjs/common';
import { ErrorUtilService } from './error-util.service';
import { PaginationService } from './pagination.service';

@Global()
@Module({
  providers: [ErrorUtilService, PaginationService],
  exports: [ErrorUtilService, PaginationService],
})
export class SharedModule {}
