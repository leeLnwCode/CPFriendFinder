# Larger room call view, screen sharing and temporary-link gateway

Completed 9 October 2026. No Git commands or PR actions were used. Excel was not modified.

## Product behavior

Room participants have selectable video thumbnails and a larger 16:9 stage with a fullscreen control. Multiple participants can share simultaneously; select any participant to watch their share. Shared content uses contain sizing so text is not cropped. The mobile layout retains a full-width stage and smaller thumbnails.

The share button calls the browser's getDisplayMedia picker directly from a user click. The browser lets the user select a screen/window/tab where supported; permissions cannot be silently bypassed or preselected by the app. Capture shares video only; microphone audio remains active. During sharing the outgoing camera video is replaced by the selected screen track. Stopping capture restores the original camera track and its enabled state, or returns to voice-only when no camera existed. Browser stop-sharing events and picker cancellation are handled; leaving the call stops both screen and camera tracks.

server.js is a dependency-free Node gateway on 127.0.0.1:3000, forwarding HTTP/session cookies and WebSocket signaling to the existing Spring application at 127.0.0.1:8080. It does not replace Spring, PostgreSQL, authentication, or room membership. The authenticated /api/call/ice-config endpoint supplies optional TURN configuration from environment variables. Call clients retain STUN fallback when no gateway config is available.

## Running locally

Start the existing Spring application through the established workflow, then run:

```powershell
Set-Location 'C:\Users\User\Desktop\CPFriendFinder-NEW'
node server.js
```

Visit http://localhost:3000. This gateway is currently running from the current task; a second instance would conflict on port 3000. The attempted separate Spring launch found port 8080 already occupied, so it was not kept running. Live read-only requests through the gateway returned HTTP 200 for /login and /js/room-call.js and served the new share control. Existing application sessions remain handled by Spring; login separately when changing the host to a public tunnel URL.

Optional gateway settings: BACKEND_URL (loopback HTTP only), PORT, TURN_URLS (comma-separated turn:/turns: endpoints), TURN_USERNAME, TURN_CREDENTIAL. Supply real TURN account credentials as environment variables, not in client source or committed files. No TURN credentials were provisioned in this task. No relay traffic or cross-network connection has been validated.

## Temporary public link

The portable official cloudflared executable is downloaded to the chat workspace under tools/cloudflared.exe. A Quick Tunnel would forward the gateway on port 3000 and print an HTTPS trycloudflare.com URL. This publishes the app through a public address while retaining the app's existing login requirements; it is not an additional access-control service. The address lasts while the tunnel and local gateway/backend remain running.

The automatic approval review rejected opening this tunnel because it would expose the backend/app and returned data through an as-yet-unknown public URL. No tunnel was opened, no public URL was generated, and no workaround was attempted. Explicit user approval of publishing the current application/data through this temporary tunnel is pending.

An HTTPS tunnel makes the website and signaling reachable, but does not relay WebRTC audio/video. TURN is separately needed for restrictive NAT/firewall cases. The current configuration is STUN-only and must not be represented as verified cross-network support. Once a TURN service is configured, validate from two independent networks and inspect relay-selected ICE candidates.

## Verification evidence

- Maven command: mvn -o '-Dtest=CallFrontendContractTest,FriendNotificationChatContractTest' test. 18 tests passed, zero failures/errors/skips; elapsed 5.117 seconds; completed 2026-10-09 04:11:22 +07:00. Only these suites were rerun for this frontend/gateway change.
- screen-share-verification.json: 7 checks passed. Two simultaneous shares, stage switching and native decoded video frames, fullscreen, restoration of original camera and track release, browser ended event, picker cancellation preserving audio/call, mobile fit.
- group-call-verification.json: 7 group-call regression checks passed with three accounts.
- call-regression-verification.json: all 10 existing friend-call flows passed.
- gateway-verification.json: 4 checks passed: HTTP/cookie passthrough, authentication required for ICE credentials, environment-provided TURN/no-store response, native WebSocket upgrade and bidirectional signaling.

Screen tests replace the display picker with a test double returning browser test-device video; WebRTC and decoded frames are native. They do not verify a physical monitor picker, tab audio, real TURN service or independent networks. Gateway tests use a test backend and test-only TURN values. Raw reports are copied outside the repo for the existing Excel recording plan; do not combine overlapping historical runs into a new suite total.

References: https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getDisplayMedia ; https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/ ; https://webrtc.org/getting-started/turn-server
