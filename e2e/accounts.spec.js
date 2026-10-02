const { test, expect } = require('@playwright/test');

test('directory, industry filter, search and detail', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Accounts', exact: true })).toBeVisible();
  await expect(page.locator('tbody tr').first().getByRole('link')).toBeVisible();
  for (const badge of await page.locator('tbody .risk-badge').all()) {
    await expect(badge).toHaveText(/^(Low|Medium|High) · \d+\/100$|^Unknown · Not scored$/);
    await expect(badge).toHaveAttribute('aria-label', /^Risk: /);
  }
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
  await expect(page.locator('.risk-badge')).toBeVisible();
  await expect(page.locator('.risk-badge')).toHaveAttribute('aria-label', /^Risk: /);
});

// Run against the producer + API contract, with at least one scored high-risk account.
test('high-risk filter survives industry, search and pagination', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('Risk filter').selectOption('true');
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page).toHaveURL(/high_risk=true/);
  await expect(page.locator('tbody .risk-badge').first()).toBeVisible();
  const assertHighRiskRows = async () => {
    for (const badge of await page.locator('tbody .risk-badge').all()) {
      await expect(badge).toHaveText(/^High · (?:[7-9]\d|100)\/100$/);
      await expect(badge).toHaveClass(/risk-high/);
    }
  };
  await assertHighRiskRows();
  const industry = await page.locator('tbody tr').first().locator('td').nth(1).innerText();
  await page.getByLabel('Industry', { exact: true }).selectOption(industry);
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page.getByLabel('Risk filter')).toHaveValue('true');
  await assertHighRiskRows();
  const next = page.getByRole('link', { name: 'Next', exact: true });
  if (await next.count()) {
    await next.click();
    await expect(page).toHaveURL(/high_risk=true/);
    await expect(page.getByLabel('Industry', { exact: true })).toHaveValue(industry);
    await expect(page.getByLabel('Risk filter')).toHaveValue('true');
    await assertHighRiskRows();
    await page.getByRole('link', { name: 'Previous', exact: true }).click();
  }
  const name = await page.locator('tbody tr').first().getByRole('link').innerText();
  await page.getByLabel('Account name').fill(name);
  await page.getByRole('button', { name: 'Apply filters' }).click();
  await expect(page.getByLabel('Risk filter')).toHaveValue('true');
  await expect(page.getByLabel('Industry', { exact: true })).toHaveValue(industry);
  await expect(page.locator('tbody tr')).toHaveCount(1);
  await assertHighRiskRows();
  await page.getByRole('link', { name, exact: true }).click();
  await expect(page.locator('.risk-badge')).toHaveText(/^High · (?:[7-9]\d|100)\/100$/);
});
