# TS003 - Authentication Controller MVC Test Cases

**Test Class:** `AuthControllerMvcTest`  
**Method:** Standalone MockMvc with `AuthService` mocked  
**Execution Date:** 2026-09-30  
**Result:** 10/10 passed in the 34-test full-suite run

| Test Case ID | Endpoint | Test Condition | Expected Result | Status |
|---|---|---|---|---|
| CTRL-001 | POST /api/auth/register | Valid registration JSON; service returns a user | 201; public user fields only | Pass |
| CTRL-002 | POST /api/auth/register | Request body omitted | 400; service not called | Pass |
| CTRL-003 | POST /api/auth/register | Malformed JSON | 400; service not called | Pass |
| CTRL-004 | POST /api/auth/login | Valid credentials; service returns a user | 200; userId stored in session | Pass |
| CTRL-005 | POST /api/auth/login | Request body omitted | 400; service not called | Pass |
| CTRL-006 | POST /api/auth/login | Malformed JSON | 400; service not called | Pass |
| CTRL-007 | GET /api/auth | Use session from successful login | 200; matching userId | Pass |
| CTRL-008 | GET /api/auth | Session without prior login | 200; userId null (current controller behavior) | Pass |
| CTRL-009 | POST /api/auth/logout | Existing session containing userId | 204; session invalidated | Pass |
| CTRL-010 | POST /api/auth/logout | No prior login | 204 | Pass |

See the workbook for JUnit methods, requirement references and case-level execution details.
