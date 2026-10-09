*** Settings ***
Resource         ../resources/common.resource
Suite Setup      Open CP Friend Finder
Suite Teardown   Close CP Friend Finder

*** Test Cases ***
E2E-001 Login Page Loads
    Go To    ${BASE_URL}/login
    Get Title    ==    Login - CP Friend Finder
    Get Element    id=email
    Get Element    id=password
    Get Element    id=showPassword
    Get Element    css=.login-button

E2E-002 Password Can Be Shown And Hidden
    Go To    ${BASE_URL}/login

    Get Attribute    id=password    type    ==    password

    Click    id=showPassword
    Get Attribute    id=password    type    ==    text
    Get Text    id=showPassword    ==    Hide

    Click    id=showPassword
    Get Attribute    id=password    type    ==    password
    Get Text    id=showPassword    ==    Show

E2E-003 Register Link Opens Register Page
    Go To    ${BASE_URL}/login
    Click    css=.signup-link a
    Get Url    ==    ${BASE_URL}/register
