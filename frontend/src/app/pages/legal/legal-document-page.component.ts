import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { NavbarComponent } from '../../components/navbar/navbar.component';

const LEGAL_PARAGRAPH_KEYS: Record<string, string[]> = {
  terms: ['legal.terms.p1', 'legal.terms.p2', 'legal.terms.p3', 'legal.terms.p4', 'legal.terms.p5', 'legal.terms.p6', 'legal.terms.p7'],
  privacy: [
    'legal.privacy.p1',
    'legal.privacy.p2',
    'legal.privacy.p3',
    'legal.privacy.p4',
    'legal.privacy.p5',
    'legal.privacy.p6',
    'legal.privacy.p7',
    'legal.privacy.p8',
  ],
};

@Component({
  selector: 'app-legal-document-page',
  imports: [NavbarComponent, TranslatePipe],
  templateUrl: './legal-document-page.component.html',
  styleUrl: './legal-document-page.component.scss',
})
export class LegalDocumentPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  titleKey = 'legal.terms.title';
  paragraphKeys: string[] = LEGAL_PARAGRAPH_KEYS['terms']!;

  ngOnInit(): void {
    const doc = this.route.snapshot.data['legalDoc'] as string;
    const keys = LEGAL_PARAGRAPH_KEYS[doc];
    if (keys != null) {
      this.paragraphKeys = keys;
      this.titleKey = `legal.${doc}.title`;
    }
  }
}
