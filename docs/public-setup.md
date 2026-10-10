# Public link startup repair — 9 October 2026

The previous check verified /login, but the supplied root URL / returned a blank HTTP 403 from the running backend. Cloudflare and the gateway were reachable. PageController had no root mapping, so error dispatch fell through security handling.

server.js now redirects GET/HEAD / to /login immediately, provides /health with backend readiness and relay status, rewrites absolute backend redirects to relative paths, and times out stalled upstream HTTP requests. PageController also maps / to redirect:/login for subsequent backend restarts. The gateway was restarted in task session 83521; the existing approved Cloudflare tunnel in session 60843 continues running with the same public URL.

Public URL: https://life-train-operator-emily.trycloudflare.com/login

start-sharing.ps1 checks the Spring backend and gateway before showing readiness. It starts missing processes with hidden windows and writes logs under %LOCALAPPDATA%/CPFriendFinder/runtime. Running it locally succeeded: Ready: http://localhost:3000/login. Its -Public option downloads official portable cloudflared if absent, creates a temporary public tunnel, waits for the remote login page, and prints the URL. Public mode was syntax-checked; the already-running public tunnel was retained, rather than creating another address. Users running Public mode should expect a new temporary URL.

Usage from the project folder:

```powershell
.\start-sharing.ps1
# To open a new approved public tunnel:
.\start-sharing.ps1 -Public
```

The current backend on port 8080 was reused and not killed/restarted. Maven copied the updated static resources to target/classes; the new Java entry mapping takes effect after backend restart. The gateway root fix works immediately with the existing backend.

Validation:

- mvn -o '-Dtest=PageControllerMvcTest,FrontendAuthContractTest' test: 9 tests, zero failures/errors/skips. PageControllerMvcTest 3 and FrontendAuthContractTest 6. Elapsed 23.100 seconds; completed 2026-10-09 04:25:39 +07:00.
- gateway-verification.json: 6 checks passed against a test backend, including root redirect, readiness, HTTP/session passthrough, authenticated ICE config and native WebSocket signaling.
- artifacts/public-setup/verification.json: 7 checks passed through the LIVE public tunnel in a real browser: root reaches visible login, CSS/JS load, backend health ready, anonymous user API rejected, invalid login credentials rejected, registration page present, and mobile layout fits. No page errors. Invalid login POST returned HTTP 400 with Invalid email or password, with and without the public Origin header. No account was created or successfully logged in.
- login-mobile.png: screenshot of the public mobile login page.

During the first live browser run an API check encountered an unexpected status while compilation was in progress; later direct requests and the completed browser run confirmed HTTP 401 for anonymous /api/users/me. The registration check was corrected to inspect its actual step-based inputs/buttons instead of assuming a form element. These are not counted as successful initial runs.

TURN remains unconfigured. HTTP and browser readiness have been verified, but independent-network media transport and a real TURN relay have not. The tunnel is temporary and requires the local computer, backend, gateway and cloudflared processes to keep running. No Git commands, PR changes or Excel edits were performed; raw evidence is preserved separately for later recording.
