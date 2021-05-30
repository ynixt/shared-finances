import { AuthenticationComponent, registerAuthenticationStrategy } from '@loopback/authentication';
import { BootMixin } from '@loopback/boot';
import { ApplicationConfig } from '@loopback/core';
import { GraphQLBindings, GraphQLComponent } from '@loopback/graphql';
import { RepositoryMixin } from '@loopback/repository';
import { RestApplication } from '@loopback/rest';
import { RestExplorerBindings, RestExplorerComponent } from '@loopback/rest-explorer';
import { ServiceMixin } from '@loopback/service-proxy';
import path from 'path';
import { FirebaseStrategy } from './authentication-strategies/FirebaseStrategy';
import { MySequence } from './sequence';

export { ApplicationConfig };

export class SharedFinancesApplication extends BootMixin(ServiceMixin(RepositoryMixin(RestApplication))) {
  constructor(options: ApplicationConfig = {}) {
    super(options);

    // Set up the custom sequence
    this.sequence(MySequence);

    // Set up default home page
    this.static('/', path.join(__dirname, '../public'));

    // Customize @loopback/rest-explorer configuration here
    this.configure(RestExplorerBindings.COMPONENT).to({
      path: '/explorer',
    });
    this.component(RestExplorerComponent);
    this.component(GraphQLComponent);
    this.configure(GraphQLBindings.GRAPHQL_SERVER).to({
      asMiddlewareOnly: true,
    });
    this.component(AuthenticationComponent);
    registerAuthenticationStrategy(this, FirebaseStrategy);

    const server = this.getSync(GraphQLBindings.GRAPHQL_SERVER);
    this.expressMiddleware('middleware.express.GraphQL', server.expressApp);

    this.projectRoot = __dirname;
    // Customize @loopback/boot Booter Conventions here
    this.bootOptions = {
      controllers: {
        // Customize ControllerBooter Conventions here
        dirs: ['controllers'],
        extensions: ['.controller.js'],
        nested: true,
      },
      graphqlResolvers: {
        // Customize ControllerBooter Conventions here
        dirs: ['graphql-resolvers'],
        extensions: ['.js'],
        nested: true,
      },
    };
  }
}
