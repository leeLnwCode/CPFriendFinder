# Evidence scope

Robot logs contain only disposable test accounts/passwords, not user credentials. `robot-isolated-results` is the initial environment-startup failure (connection refused, 0/5), `robot-isolated-retest` is the successful 5/5 retest, and `robot-final-results` is the later successful 5/5 run after WebSocket authorization changes. These earlier runs preceded the final direct-room concurrency fix; it is covered by the separately timestamped realtime-isolated result. A final auth Robot run is recorded separately when available. All use localhost and an ephemeral H2 database, not the production PostgreSQL/storage/TURN stack. Do not relabel them as physical cross-network acceptance. Workbook history is preserved unchanged.

Java evidence is sanitized into doc/review/java-test-summary.json; raw JUnit system properties and backend logs are excluded because they can expose machine/runtime configuration. Native browser fixture and actual HTTP/STOMP evidence each have their own scope in doc/review. Historical failed Java results are preserved in doc/history/before-final-fixes-java-test-summary.json.

`robot-release-results` is the final 5/5 auth run after the direct-room race and handshake fixes.
