/**
 * MIC AI secure backend proxy — Cloudflare Worker example.
 *
 * Why this exists: MIC AI must never ship an API key inside the APK.
 * Instead, deploy this worker once, put your AI provider key in the
 * worker's secrets, and point MIC AI (Settings → API base URL) at your
 * worker URL. The APK only ever talks to YOUR proxy.
 *
 * Deploy:
 *   1. Create a free Cloudflare account + Worker.
 *   2. Paste this file into the worker.
 *   3. Add a secret named AI_API_KEY (provider key) and optionally
 *      AI_BASE_URL (default: OpenAI) and AI_MODEL.
 *   4. In MIC AI → Settings, set:
 *        API base URL = https://your-worker.your-subdomain.workers.dev/v1
 *        Model        = gpt-4o-mini (or your provider's model)
 *        API key      = (leave blank — the worker injects the real key)
 *
 * The worker only accepts POST /v1/chat/completions and forwards it.
 * It does not log request bodies or keys.
 */

const DEFAULT_BASE_URL = "https://api.openai.com/v1";
const DEFAULT_MODEL = "gpt-4o-mini";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders() });
    }
    if (url.pathname !== "/v1/chat/completions" || request.method !== "POST") {
      return json({ error: "Not found. Use POST /v1/chat/completions." }, 404);
    }

    const apiKey = env.AI_API_KEY;
    if (!apiKey) {
      return json({ error: "Server misconfigured: AI_API_KEY secret is missing." }, 500);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "Invalid JSON body." }, 400);
    }

    // Pin the model server-side so clients cannot burn quota on expensive models.
    body.model = env.AI_MODEL || DEFAULT_MODEL;

    const baseUrl = (env.AI_BASE_URL || DEFAULT_BASE_URL).replace(/\/$/, "");
    const upstream = await fetch(`${baseUrl}/chat/completions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify(body),
    });

    const text = await upstream.text();
    return new Response(text, {
      status: upstream.status,
      headers: { "Content-Type": "application/json", ...corsHeaders() },
    });
  },
};

function json(payload, status) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json", ...corsHeaders() },
  });
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
  };
}
