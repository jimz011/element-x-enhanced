# Matrix Classic

Matrix Classic is the Android app distributed as Element X Enhanced, a personal fork of Element X with public source. It deliberately keeps the Matrix Rust SDK, encryption, room timeline, calls, and attachment
plumbing from upstream and limits custom code to the personal UX and media-compatibility requirements.

## Included customisations

- A search button in each chat's top bar opens **Search in this chat**. Keyword results are limited to that chat and tapping a result jumps to the
  original message. Global search stays available separately. Both searches use the account's local message index, so results depend on the history
  already downloaded and indexed on the device.
- A space switcher reached from its own floating button, stacked above the compose button at the bottom right, as in Element
  Classic. Element X's own Chats/Spaces tab bar is removed. The sheet lists **All chats** and **Direct messages** as fixed
  destinations, then the spaces themselves; a space with children shows a chevron to expand or collapse them (collapsed by
  default), and selecting a parent space shows every chat from it and all of its child spaces, recursively.
- Unread-room-count badges, like classic: one on the space switcher button itself (the account-wide total), and one per space
  inside the sheet, aggregated the same way a parent space's chat list is (a space's badge includes unread rooms that only live
  in one of its children).
- A **Recent** shortcut row showing the account's most recently active chats, newest first -- including a message someone else
  just sent, not only chats this device has personally opened. It reads the same recency-ordered list the SDK already hands back,
  so it never needs its own "did I open this" bookkeeping. Unlike the room list below it, this row always reflects every room in
  the account: it is never narrowed by the current category or space filter, and it keeps showing even when that filter's results
  are empty.
- Filter chips (Unread / People / Rooms / Favourites / Invites) sit below the Recent row, above the room list, as in classic.
  There is no separate Favourites section in the list any more -- the Favourites chip is the only favourites entry point, so a
  favourited room just shows in its normal People/Rooms section.
- The room list is split into the classic sections, in this order: Invites, People, Rooms, Low priority. Headers are hidden when
  only one section has rooms in it, so filtered views stay uncluttered.
- A **Mark all as read** entry in the room list's overflow menu, matching classic/desktop. This exists mainly to compensate for
  `numUnreadMessages`: the bundled Rust SDK counts some non-message state events (e.g. a contact's avatar or display name change)
  as "unread", which classic does not. That counting logic lives in the Rust SDK, not this Kotlin layer, so it cannot be corrected
  here directly -- the bulk action is the practical workaround.
- HEIC and HEIF images are always decoded and re-encoded as JPEG before upload, regardless of the general image-optimisation setting.
- Videos are always exported as MP4 with H.264 video and AAC audio. HDR input is tone-mapped to SDR with Media3's OpenGL tone mapper.
- A separate application ID (`io.github.jimz011.matrixclassic`) and app name, so the app can coexist with official Element clients.
- A separate OAuth identity hosted on `jimz011.github.io`, so the custom redirect scheme does not clash with official Element clients.
- Only the `arm64-v8a` ABI is built. No `armeabi-v7a`, `x86` or `x86_64` APK is produced and no universal APK is produced.
- Firebase push is excluded because this separate application ID has no Firebase registration. UnifiedPush remains enabled.

## Local build

Install Android Studio and its Android SDK, then set `JAVA_HOME` to Android Studio's bundled JBR. On Windows:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleFdroidDebug
```

The APK is written to `app/build/outputs/apk/fdroid/debug/app-fdroid-arm64-v8a-debug.apk`. Debug builds use the repository's debug key and are suitable for personal
sideloading. Before treating an APK as a long-lived release, configure a private release keystore and keep both the keystore and its passwords
outside Git; losing the key prevents installing future upgrades over the existing app.

## OAuth client metadata

Element X registers itself with a homeserver's authentication service using OAuth dynamic client registration. The Matrix Authentication Service
rejects the registration with `invalid redirect_uri` unless the app's custom redirect scheme reverse-DNS matches the host of the `client_uri` it
sends, and unless the logo, terms and policy URIs sit on that same host. Because this fork uses the redirect scheme `io.github.jimz011`, every one of
those URLs must live on `jimz011.github.io`. They are configured together in `plugins/src/main/kotlin/config/BuildTimeConfig.kt`:

| Field | Value |
| :--- | :--- |
| `METADATA_HOST_REVERSED` | `io.github.jimz011` (redirect scheme; debug builds append `.debug`) |
| `URL_WEBSITE` + `OAUTH_CLIENT_URL_PATH` | `https://jimz011.github.io/apps/android` (`client_uri`) |
| `URL_LOGO` | `https://jimz011.github.io/apps/android/logo.png` |
| `URL_ACCEPTABLE_USE` | `https://jimz011.github.io/apps/android/acceptable-use-policy` |
| `URL_POLICY` | `https://jimz011.github.io/apps/android/privacy` |

The authentication service does not fetch these URLs, so login works whether or not the pages exist. It does show the logo and links on its consent
screen, and `URL_ACCEPTABLE_USE` and `URL_POLICY` are also the in-app legal links under Settings, so publishing those three pages on the GitHub Pages
site is worthwhile. If any of these values is changed, all of them must keep the same host or login breaks again.

## Push notifications

Use a UnifiedPush distributor such as ntfy or Sunup on each phone and configure it inside Matrix Classic. A Firebase build cannot receive push until
this package name is registered in a Firebase project and the matching configuration is supplied.

## Updating from Element X

The fork now includes stable tag `v26.09.4` (checked against the official GitHub releases on 11 October 2026). This includes the unread room activity
hotfix and upstream search history alongside the custom message search filters. Use the update helper to discover GitHub's latest stable release,
fetch its tag and report the changes:

```powershell
powershell -ExecutionPolicy Bypass -File tools/upstream/update-element-x.ps1
```

The default is fetch/report only and works with uncommitted customisations. To integrate a release, first commit or stash your changes (include
untracked files), then run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/upstream/update-element-x.ps1 -Merge
```

This prepares a normal Git merge without committing or rewriting history. Resolve conflicts preserving **Included customisations**, run the focused
home, media and navigation tests, build the APK, then commit with a title and description. Restore any stash and check again. `git merge --abort`
cancels a pending merge. The script skips screenshot LFS downloads because upstream may not make all snapshot objects available to forks; CI records
new screenshots. Generated localisation updates come from the upstream merge; custom strings still belong in `temporary.xml`.

Stable releases are the default. `-IncludePrerelease` opts into the newest published prerelease; `-Tag v26.09.4` pins a specific release.
`-Tag v26.09.2 -NoFetch` uses a tag already fetched locally. Always merge the complete release so the Kotlin core, Rust SDK bindings, resources and
build configuration stay compatible.

## Foldable and tablet layout

The chat list and selected conversation appear side by side in windows at least 600 dp wide and 400 dp tall. The list uses 280?360 dp on a continuous
display by default. Drag the visible divider grip left or right to resize the panes; the chosen proportion survives folding and rotation.
The list stays at least 280 dp wide and the conversation at least 320 dp wide. On an occluding physical hinge, each pane stays on its own side and the
divider is fixed; a non-occluding fold allows resizing. Windows with insufficient space on either side use one pane.
Folding, resizing and rotation keep the navigation stack; narrow windows show the selected conversation full screen. Back follows the existing room
history. Settings, onboarding and account flows remain full screen. Tabletop posture uses one pane.

Before selecting a conversation, the second pane prompts you to select a chat. No additional account, room or sync instance is created.

Manual device checks: select and switch chats unfolded, type a draft, fold/unfold, rotate, resize in Android multi-window, open a notification or room
permalink, use Back, open room details/settings, and switch accounts. Check both continuous foldable screens and devices with an occluding hinge.

## Licensing

The upstream project and this fork remain subject to the repository's AGPL/commercial dual-license terms. The source is published at [Element X Enhanced](https://github.com/jimz011/element-x-enhanced) alongside the distributed app.
