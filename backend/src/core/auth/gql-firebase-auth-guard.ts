import { ExecutionContext, Injectable } from '@nestjs/common';
import { GqlExecutionContext } from '@nestjs/graphql';
import { ExecutionContextHost } from '@nestjs/core/helpers/execution-context-host';
import { AuthenticationError } from 'apollo-server-express';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class GqlFirebaseAuthGuard extends AuthGuard('firebase') {
  canActivate(context: ExecutionContext) {
    const ctx = GqlExecutionContext.create(context);
    const { req } = ctx.getContext();

    if (req.headers == null && req.Authorization != null) {
      req.headers = { authorization: req.Authorization };
    }

    return super.canActivate(new ExecutionContextHost([req]));
  }

  handleRequest(err: any, user: any) {
    if (err || !user) {
      throw err || new AuthenticationError('GqlAuthGuard');
    }
    return user;
  }
}
