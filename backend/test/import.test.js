const test = require('node:test');
const assert = require('node:assert');
const Fastify = require('fastify');
const importRoutes = require('../src/routes/import');

test('GET /health returns status ok', async () => {
  const fastify = Fastify();
  await fastify.register(importRoutes);

  const response = await fastify.inject({
    method: 'GET',
    url: '/health'
  });

  assert.strictEqual(response.statusCode, 200);
  const payload = JSON.parse(response.payload);
  assert.strictEqual(payload.status, 'ok');
});

test('POST /import validates missing url body', async () => {
  const fastify = Fastify();
  await fastify.register(importRoutes);

  const response = await fastify.inject({
    method: 'POST',
    url: '/import',
    payload: {}
  });

  assert.strictEqual(response.statusCode, 400);
});

test('POST /import accepts valid TikTok URL structure', async () => {
  const fastify = Fastify();
  await fastify.register(importRoutes);

  const response = await fastify.inject({
    method: 'POST',
    url: '/import',
    payload: {
      url: 'https://www.tiktok.com/@test/video/123456789'
    }
  });

  assert.strictEqual(response.statusCode, 200);
  const payload = JSON.parse(response.payload);
  assert.strictEqual(payload.success, true);
  assert.ok(payload.video);
});
