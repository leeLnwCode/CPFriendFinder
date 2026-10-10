*** Settings ***
Resource         ../resources/common.resource
Suite Setup      Open CP Friend Finder
Suite Teardown   Close CP Friend Finder

*** Test Cases ***
E2E-004 Register And Login Successfully
    ${suffix}=    Evaluate    __import__('uuid').uuid4().hex[:8]
    ${email}=     Set Variable    robot.${suffix}@test.com
    ${password}=  Set Variable    RobotTest123

    # Register - Step 1
    Go To    ${BASE_URL}/register
    Fill Text    id=email              ${email}
    Fill Text    id=password           ${password}
    Fill Text    id=confirmPassword    ${password}
    Click    css=#step1 .next-button

    # Register - Step 2
    Wait For Elements State    id=firstname    visible    5s
    Fill Text    id=firstname      Robot
    Fill Text    id=lastname       Tester
    Fill Text    id=dateOfBirth    2004-01-15
    Select Options By    id=year          value    3
    Select Options By    id=department    value    CS
    Click    css=#step2 .next-button

    # Register - Step 3
    Wait For Elements State    css=#step3    visible    5s
    Click    css=#step3 .interest >> nth=0
    Click    css=#step3 .next-button

    # Registration should return to login
    Wait For Elements State    id=email    visible    10s
    Wait For Load State    domcontentloaded
    Get Url    ==    ${BASE_URL}/login

    # Login with the newly-created account
    Fill Text    id=email       ${email}
    Fill Text    id=password    ${password}
    Click    css=.login-button

    # Successful login should open Home
    Wait Until Keyword Succeeds    15s    500ms    Get Url    ==    ${BASE_URL}/home