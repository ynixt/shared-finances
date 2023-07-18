import { Injectable } from '@nestjs/common';
import { Error } from 'mongoose';

@Injectable()
export class ErrorUtilService {
  async tryToGetItem(fnGet: () => Promise<any>, onCastError: (err?: Error.CastError) => any = () => null) {
    try {
      return await fnGet();
    } catch (err) {
      if (err instanceof Error.CastError) {
        return onCastError(err);
      }
      throw err;
    }
  }
}
