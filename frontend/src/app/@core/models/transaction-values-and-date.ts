export interface TransactionValuesAndDateDto {
  date: string,
  balance: number,
  expenses: number,
  revenues: number,
}

export interface TransactionValuesAndDateAndNameDto {
  date: string,
  balance: number,
  expenses: number,
  revenues: number,
  name: string,
}

export interface TransactionValuesGroupChartDto {
  values: TransactionValuesAndDateDto[]
  valuesByUser: {
    [key: number]: TransactionValuesAndDateAndNameDto[]
  }
}
