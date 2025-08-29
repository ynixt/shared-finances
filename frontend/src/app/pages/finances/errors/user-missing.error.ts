export class UserMissingError extends Error {
  constructor() {
    super('Missing user');
  }
}
