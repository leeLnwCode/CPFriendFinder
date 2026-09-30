# CPFriendFinder Test Summary Report

**Date:** 2026-09-30  
**Tester:** Jiratchaya Paocanthuek  
**Environment:** Windows 11, JDK 21, Maven Wrapper  
**Command:** `./mvnw -Djava.version=21 clean test`

## Latest Execution

**Completed:** 17:09:30 (UTC+07:00)  
**Duration:** 41.638 seconds  
**Build Result:** BUILD SUCCESS

| Test Class | Tests | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `FriendApplicationTests` | 1 | 1 | 0 | 0 | 0 |
| `AuthServiceRegisterTest` | 12 | 12 | 0 | 0 | 0 |
| `AuthServiceLoginTest` | 11 | 11 | 0 | 0 | 0 |
| `AuthControllerMvcTest` | 10 | 10 | 0 | 0 | 0 |
| `AuthSecurityWebMvcTest` | 9 | 9 | 0 | 0 | 0 |
| **Total** | **43** | **43** | **0** | **0** | **0** |

The workbook documents 42 authentication test cases: 23 service, 10 controller MVC and 9 security cases. The additional Spring application-context test brings the Maven total to 43.

## Execution History

| Completed (2026-09-30, UTC+07:00) | Scope | Result | Duration |
|---|---|---:|---:|
| 15:00:47 | Service and application context | 24/24 passed | Not recorded |
| 16:23:47 | Service, controller and application context | 34/34 passed | 55.244 seconds |
| 17:09:30 | Service, controller, security and application context | 43/43 passed | 41.638 seconds |

## Test Boundaries

Service tests use Mockito for dependencies. Controller tests use MockMvc. Security tests run the configured security filter chain with `AuthService` mocked. `FriendApplicationTests` checks application-context startup. These results do not represent end-to-end testing against a real database.

**Execution evidence:** The local Maven/Surefire report directory is `target/surefire-reports/`; case-level records and history are in the workbook.
