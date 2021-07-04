import { ChartSerie } from './chartSerie';

export class Chart {
  constructor(obj?: { name: string; series: ChartSerie[] }) {
    this.name = obj?.name;
    this.series = obj?.series;
  }

  name: string;
  series: ChartSerie[];
}
