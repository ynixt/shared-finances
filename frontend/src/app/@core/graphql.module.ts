import { NgModule } from '@angular/core';
import { APOLLO_OPTIONS } from 'apollo-angular';
import { ApolloClientOptions, InMemoryCache, split } from '@apollo/client/core';
import { HttpLink } from 'apollo-angular/http';
import { WebSocketLink } from '@apollo/client/link/ws';
import { getMainDefinition } from '@apollo/client/utilities';
import { AngularFireAuth } from '@angular/fire/auth';
import { take } from 'rxjs/operators';
import { ConnectionParams, MessageTypes, SubscriptionClient } from 'subscriptions-transport-ws';

const uri = '/api/graphql'; // <-- add the URL of the GraphQL server here
export function createApollo(httpLink: HttpLink, angularFireAuth: AngularFireAuth): ApolloClientOptions<any> {
  const http = httpLink.create({
    uri,
  });

  let alreadyConfigured = false;

  const subscriptionClient = new SubscriptionClient(`ws://localhost:3000/graphql`, {
    reconnect: true,
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

  // subscriptionClient.use([
  //   {
  //     applyMiddleware(operationOptions, next) {
  //       new Promise(async resolve => {
  //         const user = await angularFireAuth.user.pipe(take(1)).toPromise();
  //         const token = await user?.getIdToken();

  //         operationOptions['Authorization'] = token;

  //         next();
  //       });
  //     },
  //   },
  // ]);

  const ws = new WebSocketLink(subscriptionClient);

  // const ws = new WebSocketLink({
  //   uri: `ws://localhost:3000/graphql`,
  //   options: {
  //     reconnect: true,
  //     connectionParams: async () => {
  //       const user = await angularFireAuth.user.pipe(take(1)).toPromise();
  //       const token = await user?.getIdToken();

  //       return {
  //         Authorization: `Bearer ${token}`,
  //       };
  //     },
  //   },
  // });

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
    link,
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
      deps: [HttpLink, AngularFireAuth],
    },
  ],
})
export class GraphQLModule {}
