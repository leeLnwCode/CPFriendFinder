# Use case descriptions

| Use case | Precondition / actor | Main success flow | Alternate/error flow and outcome |
|---|---|---|---|
| Register and login | Visitor; unique email | Complete validated registration, return to login with email prefilled, login verifies password and establishes session | Duplicate/invalid data rejected; HTTP/network errors stay on form and permit retry |
| Edit profile | Authenticated owner | Read current details, edit biography/interests/gallery, save and reload | Maximum 5 gallery photos; only existing own URLs may be retained; invalid image/500-character biography rejected |
| Discover people | Active authenticated user | Read real registered candidates, common interests and score; exclude self/friends/pending requests | Empty list is an explicit empty state, not generated mock people; candidates need not be online for profile discovery |
| Friend request | Two different active users | Send PENDING; receiver uses current request ID to accept; create friendship and notifications | Self/duplicate friendship rejected; receiver authorization required; declined request does not create friendship |
| Rooms | Authenticated user | Create group; discover/filter/sort; join after password/capacity check; reload resumes membership | Already joined is idempotent; wrong private password/full room rejected; leaving explicitly closes membership |
| Manage owned room | Active OWNER; moderator may update per policy | Update details; owner deletes group via soft-delete and closes memberships/calls | Non-owner cannot delete; DIRECT rooms cannot be deleted using group management |
| Chat text/images | Active room member | Send text or validated image, render realtime; DIRECT history remains after reload | GROUP history is live-only; failed sends restore original draft without replacing a newer draft; image objects persist in storage |
| Edit direct text | Active member and original sender | Replace own text, broadcast EDIT on room message-updates topic | Other sender/deleted/wrong-room/media message edits rejected; group ephemeral messages have no persisted ID to edit |
| Calls and sharing | Active participants, secure browser origin, permissions | Invite/accept, ICE negotiation, voice/video toggle, browser-selected screen/window/tab and focus participant | Decline/cancel/media denial handled; TURN needed when direct connectivity fails; two-device acceptance remains manual |
| Notifications | Authenticated recipient | Read current request/message notifications and mark read | Historical request notifications cannot accept stale/deleted request IDs; refresh relationship status |
