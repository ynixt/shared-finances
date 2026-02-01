import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Provider } from '@angular/core';

const testProviders: Provider[] = [provideHttpClientTesting()];
export default testProviders;
