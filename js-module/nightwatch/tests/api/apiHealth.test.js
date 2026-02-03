/**
 * API Health Check Tests - Nightwatch.js
 * Demonstrates API testing capabilities with Nightwatch
 */

describe('API Health Check Tests', function () {
  const API_BASE_URL = process.env.API_BASE_URL || 'https://jsonplaceholder.typicode.com';

  it('should verify API is accessible', async function (browser) {
    const response = await fetch(`${API_BASE_URL}/posts/1`);

    browser.assert.ok(response.ok, 'API endpoint is accessible');
    browser.assert.equal(response.status, 200, 'API returns 200 OK');
  });

  it('should return valid JSON response', async function (browser) {
    const response = await fetch(`${API_BASE_URL}/posts/1`);
    const data = await response.json();

    browser.assert.ok(data.id, 'Response contains id');
    browser.assert.ok(data.title, 'Response contains title');
    browser.assert.ok(data.body, 'Response contains body');
    browser.assert.ok(data.userId, 'Response contains userId');
  });

  it('should handle GET request for list endpoint', async function (browser) {
    const response = await fetch(`${API_BASE_URL}/posts`);
    const data = await response.json();

    browser.assert.ok(Array.isArray(data), 'Response is an array');
    browser.assert.ok(data.length > 0, 'Response contains items');
  });

  it('should handle POST request', async function (browser) {
    const newPost = {
      title: 'Test Post',
      body: 'This is a test post body',
      userId: 1,
    };

    const response = await fetch(`${API_BASE_URL}/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(newPost),
    });

    const data = await response.json();

    browser.assert.equal(response.status, 201, 'POST returns 201 Created');
    browser.assert.equal(data.title, newPost.title, 'Title matches');
    browser.assert.equal(data.body, newPost.body, 'Body matches');
  });

  it('should handle 404 for non-existent resource', async function (browser) {
    const response = await fetch(`${API_BASE_URL}/posts/999999`);

    browser.assert.equal(response.status, 404, 'Returns 404 for non-existent resource');
  });

  it('should measure API response time', async function (browser) {
    const startTime = Date.now();

    await fetch(`${API_BASE_URL}/posts/1`);

    const responseTime = Date.now() - startTime;
    console.log(`API Response Time: ${responseTime}ms`);

    browser.assert.ok(responseTime < 2000, `Response time ${responseTime}ms is under 2000ms`);
  });

  it('should validate response headers', async function (browser) {
    const response = await fetch(`${API_BASE_URL}/posts/1`);

    browser.assert.ok(
      response.headers.get('content-type').includes('application/json'),
      'Content-Type is application/json'
    );
  });
});

describe('API CRUD Operations', function () {
  const API_BASE_URL = process.env.API_BASE_URL || 'https://jsonplaceholder.typicode.com';

  it('should perform full CRUD cycle', async function (browser) {
    // CREATE
    const createResponse = await fetch(`${API_BASE_URL}/posts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: 'CRUD Test',
        body: 'Testing CRUD operations',
        userId: 1,
      }),
    });
    const created = await createResponse.json();
    browser.assert.equal(createResponse.status, 201, 'CREATE: Resource created');
    browser.assert.ok(created.id, 'CREATE: ID assigned');

    // READ
    const readResponse = await fetch(`${API_BASE_URL}/posts/1`);
    const read = await readResponse.json();
    browser.assert.equal(readResponse.status, 200, 'READ: Resource retrieved');
    browser.assert.ok(read.id, 'READ: Resource has ID');

    // UPDATE (PUT)
    const updateResponse = await fetch(`${API_BASE_URL}/posts/1`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        id: 1,
        title: 'Updated Title',
        body: 'Updated body',
        userId: 1,
      }),
    });
    const updated = await updateResponse.json();
    browser.assert.equal(updateResponse.status, 200, 'UPDATE: Resource updated');
    browser.assert.equal(updated.title, 'Updated Title', 'UPDATE: Title changed');

    // DELETE
    const deleteResponse = await fetch(`${API_BASE_URL}/posts/1`, {
      method: 'DELETE',
    });
    browser.assert.equal(deleteResponse.status, 200, 'DELETE: Resource deleted');
  });

  it('should handle PATCH request', async function (browser) {
    const response = await fetch(`${API_BASE_URL}/posts/1`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: 'Partially Updated Title',
      }),
    });

    const data = await response.json();

    browser.assert.equal(response.status, 200, 'PATCH returns 200');
    browser.assert.equal(data.title, 'Partially Updated Title', 'Title patched');
  });
});
