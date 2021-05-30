import { dispatchers } from './dispatchers';
import { selectors } from './selectors';

export * from './dispatchers';

export const storeServices = [...dispatchers, ...selectors];
