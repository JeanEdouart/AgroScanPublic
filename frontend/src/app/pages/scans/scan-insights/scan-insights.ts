import { Component, input, output } from '@angular/core';
import { ScanInsights } from '../../../core/scan.service';

@Component({
  selector: 'app-scan-insights',
  templateUrl: './scan-insights.html',
  styleUrl: './scan-insights.scss',
})
export class ScanInsightsComponent {
  readonly insights = input.required<ScanInsights>();
  readonly plotSelected = output<string>();
  readonly diseaseSelected = output<string>();

  protected insightRatio(value: number, total: number): number {
    if (!total) return 0;
    return Math.round((value / total) * 100);
  }
}
