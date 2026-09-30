# TS005 - Web Flow and Static Resource Smoke Tests

**Project:** CPFriendFinder  
**Test class:** `WebFlowIntegrationTest`  
**Level:** Full Spring web-context smoke test  
**Status:** Not run

## Test Design

| Test Case ID | Test Scenario | Expected Result |
|---|---|---|
| WEB-001 | GET `/login` without authentication | HTTP 200; `login` view |
| WEB-002 | GET `/register` without authentication | HTTP 200; `register` view |
| WEB-003 | GET `/home` without authentication | 4xx; protected page is not accessible anonymously |
| WEB-004 | GET `/css/register.css` | HTTP 200 |
| WEB-005 | GET `/js/register.js` | HTTP 200 |
| WEB-006 | GET `/images/man.jpg` | HTTP 200 |
| WEB-007 | POST `/api/auth/register` with valid JSON | HTTP 201; public user fields; no password fields |
| WEB-008 | POST login, GET session, POST logout, GET session | HTTP 200/200/204/200; session stored then invalidated |

## Method and Scope

- Uses `@SpringBootTest` and `@AutoConfigureMockMvc` with the configured security filters.
- Replaces `AuthService` with a Mockito bean, avoiding real registration/login database operations.
- Loads the application's real Spring context, page templates, MVC mappings, and static-resource handlers.
- Does not simulate a real browser or test live PostgreSQL access.

## Execution Record

| Run Date/Time | Command | Tests | Passed | Failed | Errors | Skipped |
|---|---|---:|---:|---:|---:|---:|
| | `./mvnw clean test -Dtest=WebFlowIntegrationTest` | | | | | |

Record the observed results after executing the suite. Do not mark cases as passed before running them.
