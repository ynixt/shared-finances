import { NgModule } from '@angular/core';
import { APOLLO_OPTIONS } from 'apollo-angular';
import { ApolloClientOptions, InMemoryCache, split } from '@apollo/client/core';
import { onError } from '@apollo/client/link/error';
import { ServerError } from '@apollo/client/link/utils';
import { HttpLink } from 'apollo-angular/http';
import { WebSocketLink } from '@apollo/client/link/ws';
import { getMainDefinition } from '@apollo/client/utilities';
import { AngularFireAuth } from '@angular/fire/auth';
import { take } from 'rxjs/operators';
import { MessageTypes, SubscriptionClient } from 'subscriptions-transport-ws';
import { HotToastService } from '@ngneat/hot-toast';
import { TranslocoService } from '@ngneat/transloco';

const uri = '/api/graphql';
export function createApollo(
  httpLink: HttpLink,
  angularFireAuth: AngularFireAuth,
  hotToastService: HotToastService,
  translocoService: TranslocoService,
): ApolloClientOptions<any> {
  const errorLink = onError(({ graphQLErrors, networkError }) => {
    if (networkError != null && 'status' in networkError && (<any>networkError).status === 504) {
      translocoService
        .selectTranslate('server-offline')
        .pipe(take(1))
        .toPromise()
        .then(msg => {
          hotToastService.error(msg, {
            autoClose: false,
            dismissible: true,
          });
        });
    }
  });

  const http = httpLink.create({
    uri,
  });

  let alreadyConfigured = false;

  const subscriptionClient = new SubscriptionClient(`ws://localhost:3000/graphql`, {
    reconnect: true,
    reconnectionAttempts: 5,
    connectionParams: async () => {
      const user = await angularFireAuth.user.pipe(take(1)).toPromise();
      const token = await user?.getIdToken();

      alreadyConfigured = true;

      return {
        Authorization: `Bearer ${token}`,
      };
    },
  });

  angularFireAuth.user.subscribe(() => {
    if (alreadyConfigured) {
      subscriptionClient.close();

      // Reconnect to the server.
      subscriptionClient['connect']();

      // Reregister all subscriptions.
      Object.keys(subscriptionClient.operations).forEach(id => {
        subscriptionClient['sendMessage'](id, MessageTypes.GQL_START, subscriptionClient.operations[id].options);
      });
    }
  });

  const ws = new WebSocketLink(subscriptionClient);

  const link = split(
    // split based on operation type
    ({ query }) => {
      const def = getMainDefinition(query);
      const kind = def.kind;
      let operation = (def as any).operation;

      return kind === 'OperationDefinition' && operation === 'subscription';
    },
    ws,
    http,
  );

  return {
    link: errorLink.concat(link),
    cache: new InMemoryCache(),
    defaultOptions: {
      query: {
        fetchPolicy: 'no-cache',
      },
      mutate: {
        fetchPolicy: 'no-cache',
      },
      watchQuery: {
        fetchPolicy: 'cache-and-network',
      },
    },
  };
}

@NgModule({
  providers: [
    {
      provide: APOLLO_OPTIONS,
      useFactory: createApollo,
      deps: [HttpLink, AngularFireAuth, HotToastService, TranslocoService],
    },
  ],
})
export class GraphQLModule {}
