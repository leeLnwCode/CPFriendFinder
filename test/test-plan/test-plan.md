# CPFriendFinder Test Plan

**Course:** CP353002 Principles of Software Design and Development  
**Tester:** Jiratchaya Paocanthuek  
**Prepared:** 2026-09-30

## 1. Objective

Verify the registration and login features of CPFriendFinder and maintain test cases, execution results, and traceable defect records.

## 2. Scope and Execution Status

| Component | Test Method | Latest Result |
|---|---|---:|
| Registration service | JUnit 5 and Mockito | 12/12 passed |
| Login service | JUnit 5 and Mockito | 11/11 passed |
| Authentication controller | Standalone MockMvc | 10/10 passed |
| Controller with Spring Security | `@WebMvcTest`, `SecurityConfig`, MockMvc | 9/9 passed |
| Spring application context | `@SpringBootTest` | 1/1 passed |
| **Total** | | **43/43 passed** |

## 3. Test Design

Equivalence Class testing covers valid, missing, and invalid inputs. Boundary Value Analysis covers seven- and eight-character registration passwords. Source-based cases check account status and email normalization. Controller and security tests verify HTTP responses, JSON and session behavior.

Service tests mock the repository and password encoder. Controller and security tests mock `AuthService`. Individual test cases and requirement references are recorded in `test/test-cases/CPFriendFinder_TestCases.xlsx`.

## 4. Environment

| Item | Configuration |
|---|---|
| Operating system | Windows 11 |
| Terminal | Git Bash |
| Java | Eclipse Temurin JDK 21 |
| Testing tools | Maven Wrapper, JUnit 5, Mockito and MockMvc |
| Command | `./mvnw -Djava.version=21 clean test` |

## 5. Pass Criteria and Records

A case passes when its assertions match the expected results without test failures or errors. The workbook contains case-level status and requirement traceability; `test/reports/test-summary.md` contains full-suite results. Confirmed defects, if any, are recorded in the **Defect Summary** sheet.

## 6. Not Covered by These Results

The 43 passing tests do not establish successful real-database integration, end-to-end behavior, or user acceptance testing. Those activities require separate tests and execution records.
