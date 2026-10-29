# Beta testing noduS

noduS is an experimental Android launcher. Early feedback is welcome, including
from older phones running Android 8.0 or newer. This GitHub beta is separate from
any future Google Play closed test; installing a GitHub APK does not enroll you
in a Play test.

## Download and install

Open [GitHub Releases](https://github.com/issu-lab/nodus/releases) and choose
the newest beta package. The first beta is
[0.1.18 beta 1](https://github.com/issu-lab/nodus/releases/tag/v0.1.18-beta.1). Download `nodus-<version>-beta.zip`, extract it and open
the included APK on your phone. The ZIP includes the app, installation notes and
license notices. A SHA-256 file accompanies the download for integrity checks.

Allow installation from the browser or file manager you use, if Android asks.
Select **noduS launcher** in Android's default Home app settings. Keep your
previous launcher installed so you can switch back from those settings.

The initial package uses the debug-signed 0.1.18 APK. It can
update the existing 0.1.17 test installation because the signing certificate is
the same. Future packages must preserve the signing identity to update in place;
a signing change requires a separate migration plan. Do not uninstall an existing
installation just to apply an update: uninstalling removes app settings.

The beta remains experimental; device gesture and notification checks are ongoing.

## Enable only what you want to test

| Feature | Access needed |
|---|---|
| App launching and basic home | Default Home selection |
| Hub, replies and media sessions | Notification access in Android settings |
| Suggested apps | Optional usage access; alphabetical fallback without it |
| Calendar widgets on home or hub | Calendar permission and selected calendars |
| Weather | Optional approximate location; disabled initially |
| Custom fonts | Choose your own TTF/OTF through Android's file picker |

## Useful test cases

- Open/close all apps with slow drags, fast flings, cancelled gestures and Back.
  Check system navigation still works and the home stays fixed. Scroll back to the
  first apps and continue pulling down to close the drawer.
- Try small/large fonts, original icons/dots and spacing at 0, 20 and 32 dp.
- Check battery percentage after leaving/returning, screen off/on and charging.
- Receive a notification, open its conversation, reply if supported and dismiss it.
  Verify the result in Android's notification shade as well.
- Try music/calendar on home, hub, both and hidden, including permission refusal.
- For music, distinguish pause with an active media notification, notification
  removal, removing the player from Recents and force-stopping it. A paused widget
  is allowed only while an active media notification exists.
- Check today's ongoing/all-day calendar events and widget dismissal by swipe.

The current device checks and limitations remain in [ROADMAP.md](ROADMAP.md).
Passing a test on one phone does not establish compatibility with every device.

## Report feedback

Use the [Beta feedback issue form](https://github.com/issu-lab/nodus/issues/new/choose).
Include app version, phone model, Android version, navigation mode (gestures or
buttons), relevant settings, exact steps, expected result and actual result.
For motion problems, a short screen recording is more useful than a still image.

Remove message contents, contact names, calendar details, location and other
personal information from attachments. Do not upload full notification exports,
credentials, private font files or an unreviewed system log.

## Invitation draft (Italian)

> Sto cercando persone interessate a provare noduS, un launcher Android minimale
> con un hub per le notifiche. È una beta sperimentale, non ancora una versione
> stabile. Mi interessano riscontri su fluidità, notifiche, widget e comportamento
> su telefoni diversi, anche meno recenti (Android 8 o superiore). Le versioni
> saranno disponibili nelle prerelease GitHub, con istruzioni e un modulo per
> segnalare problemi. Se ti interessa, dimmi modello del telefono e versione Android.

Repository: https://github.com/issu-lab/nodus . This text is a draft;
no invitations have been sent automatically.
