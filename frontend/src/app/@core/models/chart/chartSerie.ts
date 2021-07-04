export class ChartSerie {
  constructor(obj?: { name: string; value: number }) {
    this.name = obj?.name;
    this.value = obj?.value;
  }

  name: string;

  value: number;
}
