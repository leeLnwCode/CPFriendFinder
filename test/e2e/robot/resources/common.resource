*** Settings ***
Library    Browser

*** Variables ***
${BASE_URL}    http://localhost:8080
${BROWSER}     chromium

*** Keywords ***
Open CP Friend Finder
    New Browser    ${BROWSER}    headless=False
    New Context    viewport={'width': 1440, 'height': 900}
    New Page    about:blank

Close CP Friend Finder
    Close Browser
