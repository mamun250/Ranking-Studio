const { downloadTikTokVideo } = require('../services/tiktokDownloader');

async function importRoutes(fastify, options) {
  // POST /import
  fastify.post('/import', {
    schema: {
      body: {
        type: 'object',
        required: ['url'],
        properties: {
          url: { type: 'string', minLength: 5 }
        }
      },
      response: {
        200: {
          type: 'object',
          properties: {
            success: { type: 'boolean' },
            video: { type: 'string' },
            fileId: { type: 'string' },
            message: { type: 'string' }
          }
        }
      }
    }
  }, async (request, reply) => {
    const { url } = request.body;
    fastify.log.info({ url }, 'Processing TikTok import request');

    try {
      const result = await downloadTikTokVideo(url);
      const host = request.headers.host || 'localhost:3000';
      const protocol = request.protocol || 'http';
      const fullVideoUrl = `${protocol}://${host}${result.downloadUrl}`;

      return reply.code(200).send({
        success: true,
        video: fullVideoUrl,
        fileId: result.fileId,
        message: 'Video downloaded and processed successfully.'
      });
    } catch (err) {
      fastify.log.error(err, 'Failed to import TikTok video');
      return reply.code(500).send({
        success: false,
        video: '',
        fileId: '',
        message: err.message || 'Internal server error during video download.'
      });
    }
  });

  // Health check endpoint
  fastify.get('/health', async (request, reply) => {
    return { status: 'ok', service: 'Ranking Studio TikTok API' };
  });
}

module.exports = importRoutes;
