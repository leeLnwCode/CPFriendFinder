# TS004 - Authentication with Spring Security

**Test Class:** `AuthSecurityWebMvcTest`  
**Method:** `@WebMvcTest` with the application `SecurityConfig`; `AuthService` mocked  
**Execution Date:** 2026-09-30  
**Result:** 9/9 passed in the 43-test full-suite run

| Test Case ID | Endpoint | Test Condition | Expected Result | Status |
|---|---|---|---|---|
| SEC-001 | POST /api/auth/register | Anonymous registration without CSRF | 201; no password fields in JSON | Pass |
| SEC-002 | POST /api/auth/login | Anonymous login without CSRF | 200; userId stored in session | Pass |
| SEC-003 | GET /api/auth | Anonymous session check | 200; userId null | Pass |
| SEC-004 | POST /api/auth/login + GET /api/auth | Login then check same session | 200; matching userId | Pass |
| SEC-005 | POST /api/auth/logout | Logout without CSRF | 204; session invalidated | Pass |
| SEC-006 | POST /api/auth/register | Malformed JSON without CSRF | 400 | Pass |
| SEC-007 | POST /api/auth/login | Missing body without CSRF | 400 | Pass |
| SEC-008 | POST /api/auth/logout | Anonymous logout without CSRF | 204 | Pass |
| SEC-009 | POST /api/auth/login | Malformed JSON without CSRF | 400 | Pass |

Results verify the configured test-slice behavior. They do not establish real-database or end-to-end behavior. See the workbook for detailed test cases.
