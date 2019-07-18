# Semux Core

[![Build Status](https://travis-ci.org/semuxproject/semux-core.svg?branch=develop)](https://travis-ci.org/semuxproject/semux-core)
[![Build status](https://ci.appveyor.com/api/projects/status/dkeif4luqj7fymi7?svg=true)](https://ci.appveyor.com/project/semuxproject/semux-core)
[![Coverage Status](https://coveralls.io/repos/github/semuxproject/semux-core/badge.svg?branch=master)](https://coveralls.io/github/semuxproject/semux-core?branch=master)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/semux/localized.svg)](https://crowdin.com/project/semux)

## What is Semux

Semux is an experimental high-performance blockchain platform that powers decentralized application. It's written purely in Java and powered by Semux BFT consensus algorithm.

More info can be found at our [Documentation page](./docs/README.md).


## Get started

1. Download the [Latest Release](https://github.com/semuxproject/semux-core/releases) and unpack it to a desired directory.
2. Run `semux-gui.bat` if you're on Windows, or `./semux-gui.sh` if you're on Linux or macOS.
3. *(Windows user) You may need to download and install [Microsoft Visual C++ 2012 Redistributable Package](https://www.microsoft.com/en-us/download/details.aspx?id=30679)  (**x64 required**)*


## Build from source

Prerequisites:
```
OpenJDK 8 or above
Apache Maven 3.5.2
```

Build:
```
git clone https://github.com/semuxproject/semux-core
cd semux
mvn install -DskipTests
```

Run:
```
./dist/linux/semux-cli.sh
```

## Contribute

Anyone is welcome to contribute to this open source project in the form of peer review, testing and patches. Please see the [contributing](./.github/contributing.md) guide for more details.

If you find a bug, please submit it to [issues](https://github.com/semuxproject/semux-core/issues).


## Wallet Localization

If you want to add new language, review/update existing translation or help to finish specific translations, you can join and do that by following link:
https://crowdin.com/project/semux


## License

[The MIT License](./LICENSE)
