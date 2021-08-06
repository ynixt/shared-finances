export interface CreditCardSummary {
  bill: number;
  expenses?: number;
  payments?: number;
  paymentsOfThisBill?: number;
  expensesOfThisBill?: number;
}
