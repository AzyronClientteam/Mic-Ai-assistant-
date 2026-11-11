# MIC AI secure backend proxy

The APK contains **no API keys**. To use the online AI brain, deploy this
Cloudflare Worker (free tier is enough) and point MIC AI at it.

## Deploy (5 minutes)

1. Create a free account at <https://dash.cloudflare.com> → **Workers & Pages** → **Create** → **Create Worker**.
2. Paste `worker.js` into the editor → **Deploy**.
3. In the worker's **Settings → Variables and Secrets**, add:
   - `AI_API_KEY` (secret) — your AI provider key (e.g. OpenAI).
   - `AI_MODEL` (optional) — e.g. `gpt-4o-mini`.
   - `AI_BASE_URL` (optional) — e.g. `https://api.openai.com/v1`.
4. In MIC AI → **Settings**:
   - API base URL: `https://<your-worker>.<your-subdomain>.workers.dev/v1`
   - Model: same as `AI_MODEL`
   - API key: leave blank (the worker injects the real key).

## Alternatives

Any OpenAI-compatible gateway works the same way (LiteLLM, OpenRouter,
a self-hosted vLLM/Ollama behind HTTPS, etc.). MIC AI only needs
`POST {baseUrl}/chat/completions` with a standard chat-completions body.

## Security notes

- The worker never logs bodies or keys.
- The model is pinned server-side so a modified client cannot request
  expensive models.
- Rotate the provider key if it ever leaks; the APK itself holds nothing
  sensitive.
