const { defineConfig } = require('@playwright/test');
if (!process.env.BASE_URL) throw new Error('Set BASE_URL to the console URL');
module.exports = defineConfig({
  testDir: '.',
  use: { baseURL: process.env.BASE_URL, browserName: 'chromium' },
});
