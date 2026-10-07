# Sheikh Tube
Android WebView browser starter with a default-on content blocker for common third-party ad/tracker hosts, search/address navigation, YouTube mobile home, and local settings.

## Build without Android Studio
Push this folder to GitHub, open Actions > Build Android APK > Run workflow, then download the SheikhTube-debug-apk artifact.

## Important limitation
The blocker filters known network hosts in pages loaded through Android WebView. It does not guarantee removal of every advertisement or sponsored element, especially first-party/server-inserted advertising.
