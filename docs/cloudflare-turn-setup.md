# Cloudflare TURN setup — 2026-10-09 Asia/Bangkok

Implemented in CPFriendFinder-NEW without Git commands:
- Private .cloudflare-turn.json holds keyId, apiToken, ttl; excluded from tracking via .gitignore. Actual keys are excluded from this evidence.
- cloudflare-turn.js generates expiring ICE credentials using the official Cloudflare generate-ice-servers endpoint. TTL configured for 86400 seconds. Provider settings reload from disk; session-specific in-memory credentials cache lasts at most five minutes. Failed requests are not cached. Provider response bodies and long-term tokens are not returned to browsers.
- server.js verifies the Spring login session before credential generation, sends no-store responses, and retains the existing HTTP/WebSocket gateway.
- turn-check.html verifies selected local relay candidates independently for both peers; remote peer-reflexive statistics no longer falsely fail a working TURN connection.

Validation:
- Four automated module tests passed: incomplete config, request authorization/schema, session isolation/cache renewal/redaction, provider/network failure handling.
- Six gateway integration checks passed using an isolated test backend: login redirect, health, HTTP/cookie proxy, authentication required, no-store ICE configuration, native WebSocket signaling.
- Actual Cloudflare API accepted the user-provided key and generated credentials. Values are not included in evidence.
- Native headless Edge WebRTC sent and echoed a random data-channel token using iceTransportPolicy=relay on both peers. Selected local candidates on both peers were relay. PASS, zero ICE errors. Full sanitized results: cloudflare-live-turn-result.json.
- Public /health: gatewayReady=true, backendReady=true, relayConfigured=true. This health flag indicates valid configured settings, not an independent relay test.
- Backend was restored using the existing startup helper; gateway restarted with the new module. Existing temporary tunnel retained.

Limits:
- Native TURN diagnostic used actual provider credentials supplied to an isolated test browser rather than a real user's authenticated app session. The route's authentication is separately covered by gateway integration checks.
- Actual voice/video/screensharing between two physical devices on separate networks still requires user testing; it was not marked passed.
- Frontend currently loads ICE config once per page session. Refresh both clients before testing and after keeping a page open beyond the 24-hour credential lifetime. Server generates new credentials for subsequent requests automatically.

User steps: refresh the application, sign in at the temporary public hostname, run /turn-check, then test two accounts on separate devices using Wi-Fi versus mobile data.
Deployment: copy server.js, turn-config.js, cloudflare-turn.js, and turn-check.html alongside the Spring app. Configure the private file or CLOUDFLARE_TURN_KEY_ID / CLOUDFLARE_TURN_API_TOKEN / CLOUDFLARE_TURN_TTL environment variables on the host. Ensure the gateway handles /api/call/ice-config and proxies the existing backend WebSocket. Long-term token remains server-side.

Official API reference: https://developers.cloudflare.com/realtime/turn/generate-credentials/
