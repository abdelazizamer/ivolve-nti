const { test } = require('node:test');
const assert = require('node:assert/strict');
const db = require('../db');

test('database helper is exported', () => {
  assert.ok(db);
});
