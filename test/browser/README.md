# Browser and HTTP regression checks

`npm install` in this directory installs the pinned Playwright dependency. `npm test` runs actual browser JavaScript against isolated API fixtures, not the deployed application. On Windows it uses installed Edge. Set `BROWSER_CHANNEL=chromium` and run `npx playwright install chromium` to use bundled Chromium. Reports are written under results/ (generated, excluded from delivery).

`python api-isolated.py http://127.0.0.1:18080` exercises actual REST APIs and creates two disposable accounts, a friendship and rooms/messages. Run it **only** on a dedicated ephemeral test database such as H2 with the provided test dependencies, never the team's normal localhost service or shared database. Localhost URL checking is not proof that a database is disposable: the operator must establish isolation first. The H2 run verifies behavior, not PostgreSQL deployment/migration or real S3/TURN media transport.

Robot cases in ../e2e/robot test registration/login in a real browser. HEADLESS defaults to True; pass --variable HEADLESS:False for a visible browser. Workbook results and physical-device media acceptance are separate steps.

`node realtime-isolated.cjs http://127.0.0.1:18080` likewise creates disposable local accounts and verifies real two-browser direct messaging, live edits and subscription authorization. It is not a physical two-device media test.
