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

test('high-risk badges, combined filters and detail return navigation', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByLabel('High-risk', { exact: true })).not.toBeChecked();
  await page.getByLabel('High-risk', { exact: true }).check();
  await page.getByRole('button', { name: 'Apply filters' }).click();
  expect(new URL(page.url()).searchParams.get('high_risk')).toBe('true');
  await expect(page.getByLabel('High-risk', { exact: true })).toBeChecked();
  const badges = page.locator('tbody .risk-badge');
  expect(await badges.count(), 'Seed at least one high-risk account in the API scoring fixture').toBeGreaterThan(0);
  for (const badge of await badges.all()) {
    await expect(badge).toHaveClass(/risk-high/);
    const match = (await badge.innerText()).match(/^High · (\d+)$/);
    expect(match).not.toBeNull();
    expect(Number(match[1])).toBeGreaterThanOrEqual(70);
    expect(Number(match[1])).toBeLessThanOrEqual(100);
  }
  // Page links must retain all current filters and the detail return offset.
  const next = page.getByRole('link', { name: 'Next', exact: true });
  if (await next.count()) {
    await next.click();
    expect(new URL(page.url()).searchParams.get('high_risk')).toBe('true');
    expect(new URL(page.url()).searchParams.get('offset')).toBe('50');
    const directoryUrl = new URL(page.url());
    await page.locator('tbody tr').first().getByRole('link').click();
    await page.getByRole('link', { name: 'All accounts' }).click();
    expect(new URL(page.url()).search).toBe(directoryUrl.search);
  }
  const row = page.locator('tbody tr').first();
  const industry = await row.locator('td').nth(1).innerText();
  const name = await row.getByRole('link').innerText();
  await page.getByLabel('Industry', { exact: true }).selectOption(industry);
  await page.getByLabel('Account name').fill(name);
  await page.getByRole('button', { name: 'Apply filters' }).click();
  const query = new URL(page.url()).searchParams;
  expect(query.get('industry')).toBe(industry);
  expect(query.get('q')).toBe(name);
  expect(query.get('high_risk')).toBe('true');
  expect(query.get('offset')).toBeNull(); // New filters restart at the first page.
  await page.getByRole('link', { name, exact: true }).click();
  await expect(page.locator('.details .risk-badge')).toHaveClass(/risk-high/);
  await page.getByRole('link', { name: 'All accounts' }).click();
  await expect(page.getByLabel('Industry', { exact: true })).toHaveValue(industry);
  await expect(page.getByLabel('Account name')).toHaveValue(name);
  await expect(page.getByLabel('High-risk', { exact: true })).toBeChecked();
  await page.getByLabel('High-risk', { exact: true }).uncheck();
  await page.getByRole('button', { name: 'Apply filters' }).click();
  expect(new URL(page.url()).searchParams.has('high_risk')).toBe(false);
  await expect(page.getByLabel('High-risk', { exact: true })).not.toBeChecked();
});
