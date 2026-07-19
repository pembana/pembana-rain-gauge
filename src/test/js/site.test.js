import test from 'node:test';
import assert from 'node:assert/strict';

import { nextTheme } from '../../main/resources/static/js/site.js';

test('theme toggle always alternates between supported themes', () => {
  assert.equal(nextTheme('light'), 'dark');
  assert.equal(nextTheme('dark'), 'light');
  assert.equal(nextTheme('unexpected'), 'dark');
});
