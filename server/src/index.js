import 'dotenv/config';
import express from 'express';
import cors from 'cors';

const app = express();
const port = Number(process.env.PORT || 8080);
const model = process.env.OPENROUTER_MODEL || 'openai/gpt-5.6';
const maxMessages = Number(process.env.MAX_MESSAGES || 40);
const maxTokens = Number(process.env.MAX_TOKENS || 2048);

app.use(cors({ origin: process.env.CORS_ORIGIN || '*' }));
app.use(express.json({ limit: '1mb' }));

app.get('/health', (_req, res) => res.json({
  ok: true, service: 'mazkiplay-agent', model, version: '0.2.0'
}));

app.post('/api/chat', async (req, res) => {
  if (!process.env.OPENROUTER_API_KEY) {
    return res.status(503).json({ error: 'OPENROUTER_API_KEY is not configured' });
  }

  const messages = Array.isArray(req.body?.messages) ? req.body.messages.slice(-maxMessages) : [];
  if (!messages.length) return res.status(400).json({ error: 'messages is required' });

  const safeMessages = messages
    .filter(m => m && ['user', 'assistant'].includes(m.role) && typeof m.content === 'string')
    .map(m => ({ role: m.role, content: m.content.slice(0, 12000) }));

  const system = {
    role: 'system',
    content: 'You are Mazkiplay Agent, a capable personal AI agent. Be concise but useful. Separate plans from completed actions. Never claim an external action happened unless a connected tool actually executed it. If a request needs a tool you do not have, say so and give the next concrete step.'
  };

  try {
    const r = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + process.env.OPENROUTER_API_KEY,
        'Content-Type': 'application/json',
        'HTTP-Referer': process.env.APP_URL || 'https://mazkiplay.ai',
        'X-Title': 'Mazkiplay Agent'
      },
      body: JSON.stringify({
        model,
        messages: [system, ...safeMessages],
        temperature: 0.2,
        max_tokens: maxTokens
      })
    });

    const data = await r.json();
    if (!r.ok) {
      return res.status(r.status).json({
        error: data?.error?.message || 'OpenRouter request failed'
      });
    }

    res.json({
      model: data.model,
      message: data.choices?.[0]?.message || { role: 'assistant', content: 'No response.' },
      usage: data.usage || null
    });
  } catch (e) {
    res.status(502).json({ error: e.message || 'AI gateway error' });
  }
});

app.listen(port, () => console.log('Mazkiplay Agent API listening on :' + port));