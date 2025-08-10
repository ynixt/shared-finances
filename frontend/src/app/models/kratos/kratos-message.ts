export interface KratosMessage {
  id: number;
  text: string;
  type: string;
  context?: Record<string, any>;
}
