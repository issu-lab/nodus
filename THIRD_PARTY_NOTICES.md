# Third-party notices

The MIT license in this repository applies to project code. It does not replace
the licenses of third-party components or fonts.

## Fonts

| Font | Use | Copyright / license |
|---|---|---|
| IBM Plex Mono | Default Android UI font | Copyright 2017 IBM Corp.; Reserved Font Name “Plex”; [SIL OFL 1.1](licenses/IBM-Plex-Mono-OFL.txt) |
| Exo 2 | Outlined logo and publication artwork; source font included | Copyright 2013 The Exo 2 Project Authors; [SIL OFL 1.1](licenses/Exo-2-OFL.txt) |
| Inter | Outlined supporting text; source font included | Copyright 2016 The Inter Project Authors in the bundled font; upstream notice also identifies 2020; [SIL OFL 1.1](licenses/Inter-OFL.txt) |

Upstream projects: [IBM Plex](https://github.com/IBM/plex),
[Exo 2](https://github.com/googlefonts/Exo-2.0) and
[Inter](https://github.com/rsms/inter).
The license copies were obtained from the corresponding Google Fonts packages.
The fonts in `source/fonts/` are unmodified copies of the shared iSSU artwork
fonts. The Android UI font is in `app/src/main/res/font/`.

The iSSU wordmark, historic S, functional symbols and Open Homelab badge come
from the shared iSSU identity assets. The noduS N uses the approved project mark.

## Weather service

Weather data is provided by [Open-Meteo](https://open-meteo.com/).
Its data attribution and service terms remain applicable; see
[Open-Meteo terms](https://open-meteo.com/en/terms).
The app uses the public endpoint without an API key. Review the applicable
service terms before distributing a build or changing its intended use.

## Android and development dependencies

AndroidX, Kotlin, Kotlin coroutines, Gradle, fontTools and Sharp are third-party
projects with their own licenses. The versions used by this project are declared
in the Gradle files and artwork tool manifests. Their upstream notices remain
applicable. This document is not a generated dependency license inventory.

The prepared GitHub beta ZIP accompanies the unchanged debug APK with this file,
the MIT license and full OFL copies. It does not add notices inside the APK itself.
Before a standalone production APK or store release, complete and verify the
distribution notices, including any applicable dependency notices.
