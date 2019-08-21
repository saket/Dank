# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]
### Added
- Subscribing to /r/all ([#28](https://github.com/Tunous/Dank/pull/28))
- View parent submission of crosspost ([#29](https://github.com/Tunous/Dank/pull/29))
- Links to repository and changelog in about screen ([#42](https://github.com/Tunous/Dank/pull/42))

### Fixed
- Clicking on notification doesn't open downloaded image ([#35](https://github.com/Tunous/Dank/pull/35))
- Notifications for downloaded videos show incorrect thumbnails ([#41](https://github.com/Tunous/Dank/pull/41))
- Video cache is not working ([#50](https://github.com/Tunous/Dank/pull/50))

## [0.7.2] - 2019-09-09
### Added
- Configured automatic releases to GitHub.

## [0.7.1] - 2019-09-09
### Fixed
- YouTube links not opening when YouTube application is not installed ([upstream#17](https://github.com/saket/Dank/pull/17))
- Login not working when user has enabled 2FA ([#32](https://github.com/Tunous/Dank/pull/32))
- Login screen not closing when user declines reddit access ([#34](https://github.com/Tunous/Dank/pull/34))

## [0.7.0] - 2019-07-09
### Added
- Highlight sticky submissions ([#16](https://github.com/Tunous/Dank/pull/16))
- Submission gesture customization ([#3](https://github.com/Tunous/Dank/pull/3))

### Changed
- Do not display submission subreddit if it's same as viewed subreddit ([9ad9e121](https://github.com/Tunous/Dank/commit/9ad9e121a2e7633e01c49c1ebf6e1b9dd114a2f0))
- Better detection for user and subreddit links ([#17](https://github.com/Tunous/Dank/pull/17))

## [0.6.3] - 2019-01-20
### Added
- Toggle to show/hide comments count on list ([#2](https://github.com/Tunous/Dank/pull/2))
- Thumbnails on the left side of submission ([#4](https://github.com/Tunous/Dank/pull/4))
- Long click options menu for in-text links ([#11](https://github.com/Tunous/Dank/pull/11))
- Sorting submissions by "Best" ([efcf181f](https://github.com/Tunous/Dank/commit/efcf181f3bd7952aa9c45c035b39bd91c26d748a))

### Changed
- Display when "Poll for new messages" is disabled ([8512307c](https://github.com/Tunous/Dank/commit/8512307cc1bb47129e674c14e6a4219beaba032a))

### Fixed
- Crash after posting comment reply ([#21](https://github.com/Tunous/Dank/pull/21))
- Crash when duplicate submissions are downloaded ([71404c12](https://github.com/Tunous/Dank/commit/71404c12fe8cee3c8770287ebef5eff52c10d724))
- Crash when clicking on thumbnail for self post ([7d54df6d](https://github.com/Tunous/Dank/commit/7d54df6dc2ed96a16a549b3ac130e53105008244))
- Pull to collapse working incorrectly in split screen ([3f1500e0](https://github.com/Tunous/Dank/commit/3f1500e0b2218f31c5a8e592082a7b8d646ca271))
- Position of option buttons in image viewer ([6d7c738c](https://github.com/Tunous/Dank/commit/6d7c738cfefbad7776f2a311770182bef6b6c6f3))
- Position of video player controls in video viewer ([97780547](https://github.com/Tunous/Dank/commit/97780547b69ab4a4c72a8be8303323f073b745b1))
- Incorrect height of videos in video viewer ([4186b3ed](https://github.com/Tunous/Dank/commit/4186b3ed2467318a3b27113a65bc791ec6c9ff8a))
- Crash when swiping item further than their width ([c9051f75](https://github.com/Tunous/Dank/commit/c9051f7586419a4bef0e856c18cb70831d43839b))
- Crash when inserting heading or quote at last empty line ([948136ef](https://github.com/Tunous/Dank/commit/948136eff59987fb68c334c1829d48d8c60123eb))
- Walkthrough screen refreshing on every action ([b20345fe](https://github.com/Tunous/Dank/commit/b20345fedfe4ee3ec4d9dc799c09c7d385db5a37))
- Prevent subreddit search field from going fullscreen ([5856531e](https://github.com/Tunous/Dank/commit/5856531e15f19e366f9556802dd90c00087d2d8e))

## [0.6.2] and earlier
Changes before the fork has been made.

[Unreleased]: https://github.com/Tunous/Dank/compare/0.7.2...HEAD
[0.7.2]: https://github.com/Tunous/Dank/compare/0.7.1...0.7.2
[0.7.1]: https://github.com/Tunous/Dank/compare/0.7.0...0.7.1
[0.7.0]: https://github.com/Tunous/Dank/compare/0.6.3...0.7.0
[0.6.3]: https://github.com/Tunous/Dank/compare/0.6.2...0.6.3
[0.6.2]: https://github.com/Tunous/Dank/releases/tag/0.6.2
