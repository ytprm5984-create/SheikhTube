# Sheikh Tube V2

Personal Android WebView app focused on the YouTube mobile site.

## V2
- Clean YouTube-first UI (URL/GO bar removed)
- Fullscreen video and Android Picture-in-Picture support
- Always-on known ad/tracker host filtering and popup protection
- Automatic GitHub Release update checks
- Branded update dialog with Update Now
- About/Developer card with Sheikh Sojib and WhatsApp 01823315984
- Loading indicator, WebView history navigation, cache clearing

## Important limitations
Filtering known hosts does not guarantee removal of every YouTube ad, especially first-party/server-inserted advertising. Background playback behavior depends on the media site/WebView and Android; the app does not bypass site restrictions.

## Updates
The updater reads the latest GitHub Release. Release APKs must always be signed with the same private signing key. Configure the repository Actions secrets before publishing a `v*` tag.
