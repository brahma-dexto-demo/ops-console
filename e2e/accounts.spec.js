const { test, expect } = require('@playwright/test');

test('directory, industry filter, search and detail', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Accounts', exact: true })).toBeVisible();
  await expect(page.locator('tbody tr').first().getByRole('link')).toBeVisible();
  await page.getByLabel('Industry', { exact: true }).selectOption('technology');
  await page.getByRole('button', { name: 'Apply filters' }).click();
  const rows = page.locator('tbody tr');
  expect(await rows.count()).toBeGreaterThan(0);
  for (const row of await rows.all()) {
    await expect(row.locator('td').nth(1)).toHaveText('technology');
  }
  const name = await rows.first().getByRole('link').innerText();
  await page.getByLabel('Account name').fill(name);
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page.locator('tbody tr')).toHaveCount(1);
  await page.getByRole('link', { name, exact: true }).click();
  await expect(page.getByRole('heading', { name, exact: true })).toBeVisible();
  await expect(page.getByText('Days since last login', { exact: true })).toBeVisible();
});
