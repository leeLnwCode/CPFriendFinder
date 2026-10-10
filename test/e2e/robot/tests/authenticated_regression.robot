*** Settings ***
Resource         ../resources/common.resource
Suite Setup      Prepare Regression Account
Suite Teardown   Close CP Friend Finder
Test Teardown    Capture Failure Evidence
Test Tags        regression    writes-test-data

*** Test Cases ***
E2E-006 Home Loads Real Rooms And Search Controls
    Go To    ${BASE_URL}/home
    Wait For Elements State    id=roomSearch    visible
    Wait For Elements State    id=interestFilter    visible
    Wait For Elements State    id=yearFilter    visible
    Wait For Elements State    id=roomSort    visible
    ${rooms}=    Read API JSON    /api/chats/discover
    Should Be True    isinstance($rooms, list)

E2E-007 Friends Page Handles Account Without Friends
    Go To    ${BASE_URL}/friend
    Wait For Elements State    id=friendSearch    visible
    ${friends}=    Read API JSON    /api/friends
    Should Be Empty    ${friends}

E2E-008 Notifications Page And API Load
    Go To    ${BASE_URL}/notification
    Wait For Elements State    id=friendRequestList    attached
    ${notifications}=    Read API JSON    /api/notifications
    Should Be True    isinstance($notifications, list)

E2E-009 Discovery Loads With Or Without Candidates
    Go To    ${BASE_URL}/random
    Wait For Elements State    id=sameYearOnly    visible
    Wait For Elements State    id=sameDepartmentOnly    visible
    ${candidates}=    Read API JSON    /api/users/discover
    Should Be True    isinstance($candidates, list)
    Wait Until Keyword Succeeds    ${UI_TIMEOUT}    500ms    Discovery Finished Successfully

E2E-010 Settings Opens Own Editable Profile
    Go To    ${BASE_URL}/setting
    Click    css=[data-modal="accountModal"]
    Click    id=settingsEditProfile
    Wait For Elements State    css=#profileModal.show    visible
    Wait For Elements State    id=saveProfileEdit    enabled
    Wait For Elements State    id=editBio    visible
    Wait For Elements State    id=editProfileInterests    visible
    Wait For Elements State    id=addGalleryPhoto    visible
    Click    id=cancelProfileEdit

E2E-011 Bio Persists After Reload
    [Documentation]    แก้เฉพาะบัญชีที่ suite สร้าง ไม่ใช้บัญชีจริงของสมาชิก
    Go To    ${BASE_URL}/home
    Open Own Profile Editor
    ${bio}=    Set Variable    ทดสอบโปรไฟล์ ${REGRESSION_EMAIL}
    Fill Text    id=editBio    ${bio}
    Click    id=saveProfileEdit
    Wait For Elements State    css=#profileModal.show    hidden
    Reload
    Open Own Profile Editor
    Get Property    id=editBio    value    ==    ${bio}
    ${me}=    Read API JSON    /api/users/me
    Should Be Equal    ${me}[bio]    ${bio}
    Click    id=cancelProfileEdit

E2E-012 Interest Removal And Addition Persist
    Go To    ${BASE_URL}/home
    Open Own Profile Editor
    ${selector}=    Set Variable    css=#editProfileInterests label:has-text("Gaming") input
    ${interest_id}=    Get Property    ${selector}    value
    Uncheck Checkbox    ${selector}
    Click    id=saveProfileEdit
    Wait For Elements State    css=#profileModal.show    hidden
    ${removed}=    Read API JSON    /api/users/me/interests
    ${ids}=    Evaluate    [str(item['id']) for item in $removed]
    List Should Not Contain Value    ${ids}    ${interest_id}
    Reload
    Open Own Profile Editor
    Get Checkbox State    ${selector}    ==    ${False}
    Check Checkbox    ${selector}
    Click    id=saveProfileEdit
    Wait For Elements State    css=#profileModal.show    hidden
    Reload
    Open Own Profile Editor
    Get Checkbox State    ${selector}    ==    ${True}
    ${restored}=    Read API JSON    /api/users/me/interests
    ${ids}=    Evaluate    [str(item['id']) for item in $restored]
    List Should Contain Value    ${ids}    ${interest_id}
    Click    id=cancelProfileEdit

E2E-013 Mobile Home Has No Horizontal Page Overflow
    Go To    ${BASE_URL}/home
    TRY
        Set Viewport Size    390    844
        Wait For Elements State    id=roomSearch    visible
        ${overflow}=    Evaluate JavaScript    ${None}    () => document.documentElement.scrollWidth > window.innerWidth + 1
        Should Not Be True    ${overflow}
    FINALLY
        Set Viewport Size    1440    900
    END

E2E-014 Logout Invalidates Session
    Go To    ${BASE_URL}/home
    Click    id=logoutButton
    Wait Until Keyword Succeeds    ${UI_TIMEOUT}    500ms    Get Url    ==    ${BASE_URL}/login
    ${status}=    Evaluate JavaScript    ${None}    async () => (await fetch('/api/users/me',{credentials:'include'})).status
    Should Be Equal As Integers    ${status}    401

E2E-015 Lost Session Returns To Login On Unauthorized API
    [Documentation]    ใช้ API จริงหลังลบ cookie ไม่ใช่การรอ idle timeout จริง
    Go To    ${BASE_URL}/login
    Fill Text    id=email    ${REGRESSION_EMAIL}
    Fill Text    id=password    ${REGRESSION_PASSWORD}
    Click    css=.login-button
    Wait Until Keyword Succeeds    ${UI_TIMEOUT}    500ms    Get Url    ==    ${BASE_URL}/home
    Delete All Cookies
    Evaluate JavaScript    ${None}    () => { fetch('/api/users/me',{credentials:'include',cache:'no-store'}).catch(()=>{}); }
    Wait Until Keyword Succeeds    ${UI_TIMEOUT}    500ms    Get Url    ==    ${BASE_URL}/login
    ${cached}=    Evaluate JavaScript    ${None}    () => sessionStorage.getItem('currentUser')
    Should Be Equal    ${cached}    ${None}

*** Keywords ***
Discovery Finished Successfully
    ${status}=    Get Text    id=discoveryStatus
    Should Match Regexp    ${status}    ^(พบบัญชีจริง|ไม่มีบัญชีที่ตรงกับตัวกรอง)

Prepare Regression Account
    Open CP Friend Finder
    ${suffix}=    Evaluate    __import__('uuid').uuid4().hex[:8]
    ${email}=     Set Variable    robot.${suffix}@example.com
    ${password}=  Set Variable    RobotTest123

    # Register - Step 1
    Go To    ${BASE_URL}/register
    Fill Text    id=email              ${email}
    Fill Text    id=password           ${password}
    Fill Text    id=confirmPassword    ${password}
    Click    css=#step1 .next-button

    # Register - Step 2
    Wait For Elements State    id=firstname    visible    ${UI_TIMEOUT}
    Fill Text    id=firstname      Regression
    Fill Text    id=lastname       Tester
    Fill Text    id=dateOfBirth    2004-01-15
    Select Options By    id=year          value    3
    Select Options By    id=department    value    CS
    Click    css=#step2 .next-button

    # Register - Step 3
    Wait For Elements State    css=#step3    visible    ${UI_TIMEOUT}
    Click    css=#step3 .interest >> nth=0
    Click    css=#step3 .next-button

    # Registration should return to login
    Wait For Elements State    id=email    visible    ${UI_TIMEOUT}
    Wait For Load State    domcontentloaded
    Get Url    ==    ${BASE_URL}/login
    Get Property    id=email    value    ==    ${email}
    Get Property    id=password    value    ==    ${password}
    Get Text    css=.registration-success    contains    สมัครสมาชิกสำเร็จ

    Set Suite Variable    ${REGRESSION_EMAIL}    ${email}
    Set Suite Variable    ${REGRESSION_PASSWORD}    ${password}
    Log    บัญชีเทสรอบนี้: ${email} (Regression Tester)

    # Login with the newly-created account
    Fill Text    id=email       ${email}
    Fill Text    id=password    ${password}
    Click    css=.login-button

    # Successful login should open Home
    Wait Until Keyword Succeeds    ${UI_TIMEOUT}    500ms    Get Url    ==    ${BASE_URL}/home
