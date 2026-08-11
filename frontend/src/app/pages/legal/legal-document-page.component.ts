import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { NavbarComponent } from '../../components/navbar/navbar.component';
import { OpenAuthPreferencesService } from '../../services/open-auth-preferences.service';

interface LegalParagraph {
  key: string;
  requiresLimits?: boolean;
  plansLink?: boolean;
}

export const LEGAL_PARAGRAPH_KEYS: Record<string, LegalParagraph[]> = {
  terms: [
    { key: 'legal.terms.p1' },
    { key: 'legal.terms.p2' },
    { key: 'legal.terms.p3' },
    { key: 'legal.terms.p4' },
    { key: 'legal.terms.p5', requiresLimits: true },
    { key: 'legal.terms.p6', requiresLimits: true, plansLink: true },
    { key: 'legal.terms.p7' },
    { key: 'legal.terms.p8', requiresLimits: true },
    { key: 'legal.terms.p9' },
    { key: 'legal.terms.p10' },
    { key: 'legal.terms.p11' },
    { key: 'legal.terms.p12' },
    { key: 'legal.terms.p13' },
  ],
  privacy: [
    { key: 'legal.privacy.p1' },
    { key: 'legal.privacy.p2' },
    { key: 'legal.privacy.p3' },
    { key: 'legal.privacy.p4' },
    { key: 'legal.privacy.p5' },
    { key: 'legal.privacy.p6', requiresLimits: true },
    { key: 'legal.privacy.p7', requiresLimits: true, plansLink: true },
    { key: 'legal.privacy.p8' },
    { key: 'legal.privacy.p9' },
    { key: 'legal.privacy.p10' },
    { key: 'legal.privacy.p11' },
    { key: 'legal.privacy.p12' },
  ],
};

@Component({
  selector: 'app-legal-document-page',
  imports: [NavbarComponent, RouterLink, TranslatePipe],
  templateUrl: './legal-document-page.component.html',
  styleUrl: './legal-document-page.component.scss',
})
export class LegalDocumentPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly openAuthPreferences = inject(OpenAuthPreferencesService);

  titleKey = 'legal.terms.title';
  paragraphs: LegalParagraph[] = [];

  ngOnInit(): void {
    const doc = this.route.snapshot.data['legalDoc'] as string;
    const keys = LEGAL_PARAGRAPH_KEYS[doc];
    if (keys != null) {
      this.paragraphs = keys.filter(entry => !entry.requiresLimits || this.openAuthPreferences.planLimitsEnabled());
      this.titleKey = `legal.${doc}.title`;
    }
  }
}
