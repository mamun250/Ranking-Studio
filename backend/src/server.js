const path = require('path');
const Fastify = require('fastify');
const cors = require('@fastify/cors');
const rateLimit = require('@fastify/rate-limit');
const fastifyStatic = require('@fastify/static');
const importRoutes = require('./routes/import');

const server = Fastify({
  logger: {
    level: process.env.LOG_LEVEL || 'info'
  }
});

async function start() {
  try {
    // Register CORS
    await server.register(cors, {
      origin: '*'
    });

    // Register Rate Limiting (100 requests per minute)
    await server.register(rateLimit, {
      max: 100,
      timeWindow: '1 minute'
    });

    // Serve static download files
    const downloadsDir = path.join(__dirname, '../downloads');
    await server.register(fastifyStatic, {
      root: downloadsDir,
      prefix: '/downloads/'
    });

    // Register API routes
    await server.register(importRoutes);

    const port = process.env.PORT || 3000;
    const host = process.env.HOST || '0.0.0.0';

    await server.listen({ port: Number(port), host });
    console.log(`🚀 Ranking Studio TikTok Backend API running at http://${host}:${port}`);
  } catch (err) {
    server.log.error(err);
    process.exit(1);
  }
}

start();
