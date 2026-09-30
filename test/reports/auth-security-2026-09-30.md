# Authentication Security Test Execution

**Date:** 2026-09-30  
**Completed:** 17:09:30 (UTC+07:00)  
**Duration:** 41.638 seconds  
**Command:** `./mvnw -Djava.version=21 clean test`  
**Build Result:** BUILD SUCCESS

| Test Class | Tests | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `FriendApplicationTests` | 1 | 1 | 0 | 0 | 0 |
| `AuthServiceRegisterTest` | 12 | 12 | 0 | 0 | 0 |
| `AuthServiceLoginTest` | 11 | 11 | 0 | 0 | 0 |
| `AuthControllerMvcTest` | 10 | 10 | 0 | 0 | 0 |
| `AuthSecurityWebMvcTest` | 9 | 9 | 0 | 0 | 0 |
| **Total** | **43** | **43** | **0** | **0** | **0** |

The security tests use the application's `SecurityConfig` in a Spring MVC test slice with `AuthService` mocked; these are not real-database integration tests.
