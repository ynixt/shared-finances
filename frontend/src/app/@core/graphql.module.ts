import { NgModule } from '@angular/core';
import { APOLLO_OPTIONS } from 'apollo-angular';
import { ApolloClientOptions, InMemoryCache, split } from '@apollo/client/core';
import { HttpLink } from 'apollo-angular/http';
import { WebSocketLink } from '@apollo/client/link/ws';
import { getMainDefinition } from '@apollo/client/utilities';
import { AngularFireAuth } from '@angular/fire/auth';
import { take } from 'rxjs/operators';

const uri = '/api/graphql'; // <-- add the URL of the GraphQL server here
export function createApollo(httpLink: HttpLink, angularFireAuth: AngularFireAuth): ApolloClientOptions<any> {
  const http = httpLink.create({
    uri,
  });

  const ws = new WebSocketLink({
    uri: `ws://localhost:3000/graphql`,
    options: {
      reconnect: true,
      connectionParams: async () => {
        const user = await angularFireAuth.user.pipe(take(1)).toPromise();
        const token = await user?.getIdToken();

        return {
          Authorization: `Bearer ${token}`,
        };
      },
    },
  });

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
