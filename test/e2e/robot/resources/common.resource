*** Settings ***
Library    Browser
Library    Collections

*** Variables ***
${BASE_URL}    http://localhost:8080
${BROWSER}     chromium
${HEADLESS}    ${False}
${UI_TIMEOUT}    40s

*** Keywords ***
Open CP Friend Finder
    New Browser    ${BROWSER}    headless=${HEADLESS}
    New Context    viewport={'width': 1440, 'height': 900}
    New Page    about:blank
    Set Browser Timeout    ${UI_TIMEOUT}

Close CP Friend Finder
    Close Browser

Read API JSON
    [Arguments]    ${path}
    ${data}=    Evaluate JavaScript    ${None}    async (path) => { const r=await fetch(path,{credentials:'include',cache:'no-store'}); if(!r.ok) throw new Error('HTTP '+r.status); return await r.json(); }    arg=${path}
    RETURN    ${data}

Open Own Profile Editor
    Click    id=sidebarProfileButton
    Wait For Elements State    css=#profileModal.show    visible
    Wait For Elements State    id=saveProfileEdit    enabled
    Get Text    id=profileEditStatus    ==    ${EMPTY}

Capture Failure Evidence
    Run Keyword If Test Failed    Run Keyword And Ignore Error    Take Screenshot    fullPage=True
