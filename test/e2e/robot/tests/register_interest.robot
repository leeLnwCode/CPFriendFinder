*** Settings ***
Resource         ../resources/common.resource
Suite Setup      Open CP Friend Finder
Suite Teardown   Close CP Friend Finder
Test Teardown    Capture Failure Evidence

*** Test Cases ***
E2E-005 Selected Interest Is Saved After Registration
    ${suffix}=    Evaluate    __import__('uuid').uuid4().hex[:8]
    ${email}=     Set Variable    robot.interest.${suffix}@example.com
    ${password}=  Set Variable    RobotTest123

    # Register - Step 1
    Go To    ${BASE_URL}/register
    Fill Text    id=email              ${email}
    Fill Text    id=password           ${password}
    Fill Text    id=confirmPassword    ${password}
    Click    css=#step1 .next-button

    # Register - Step 2
    Wait For Elements State    id=firstname    visible    ${UI_TIMEOUT}
    Fill Text    id=firstname      Interest
    Fill Text    id=lastname       Tester
    Fill Text    id=dateOfBirth    2004-01-15
    Select Options By    id=year          value    3
    Select Options By    id=department    value    CS
    Click    css=#step2 .next-button

    # Register - Step 3: select Gaming
    Wait For Elements State    css=#step3    visible    ${UI_TIMEOUT}
    Click    css=#step3 .interest >> nth=0
    Click    css=#step3 .next-button

    # Login
    Wait For Elements State    id=email    visible    ${UI_TIMEOUT}
    Wait For Load State    domcontentloaded

    Fill Text    id=email       ${email}
    Fill Text    id=password    ${password}
    Click    css=.login-button

    Wait Until Keyword Succeeds    ${UI_TIMEOUT}    500ms    Get Url    ==    ${BASE_URL}/home

    # ตรวจชื่อ Gaming ที่เลือกจริง ไม่ใช้เพียงจำนวนมากกว่าศูนย์
    ${interests}=    Read API JSON    /api/users/me/interests
    ${names}=    Evaluate    [item['name'] for item in $interests]
    List Should Contain Value    ${names}    Gaming
