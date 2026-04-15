import '@angular/compiler';
import { FormBuilder, Validators } from '@angular/forms';

import { describe, expect, it } from 'vitest';

describe('Registration consent controls', () => {
  it('requires both legal checkboxes to be true', () => {
    const fb = new FormBuilder();
    const form = fb.group({
      acceptTerms: [false, Validators.requiredTrue],
      acceptPrivacy: [false, Validators.requiredTrue],
    });

    expect(form.valid).toBe(false);

    form.patchValue({ acceptTerms: true });
    expect(form.valid).toBe(false);

    form.patchValue({ acceptPrivacy: true });
    expect(form.valid).toBe(true);
  });
});
