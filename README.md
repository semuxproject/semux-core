# Semux Core

[![Build Status](https://travis-ci.org/semuxproject/semux-core.svg?branch=develop)](https://travis-ci.org/semuxproject/semux-core)
[![Build status](https://ci.appveyor.com/api/projects/status/dkeif4luqj7fymi7?svg=true)](https://ci.appveyor.com/project/semuxproject/semux-core)
[![Coverage Status](https://coveralls.io/repos/github/semuxproject/semux-core/badge.svg?branch=master)](https://coveralls.io/github/semuxproject/semux-core?branch=master)
[![Crowdin](https://d322cqt584bo4o.cloudfront.net/semux/localized.svg)](https://crowdin.com/project/semux)

## What is Semux

Semux is an experimental high-performance blockchain platform that powers decentralized application. It's written purely in Java and powered by Semux BFT consensus algorithm.

More info can be found at our [Documentation page](./docs/README.md).


## Get started

1. Download and install [Java SE Runtime Environment](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (**x64 required**)
2. *(Windows user) Download and install [Microsoft Visual C++ 2012 Redistributable Package](https://www.microsoft.com/en-us/download/details.aspx?id=30679)  (**x64 required**)*
3. Download the [Latest Release](https://github.com/semuxproject/semux-core/releases) and unpack to a desired directory.
4. Run ``semux.exe`` if you're on Windows; run ``./semux-gui.sh`` or ``./semux-cli.sh`` if you're on Linux or macOS.


## Build from source

Prerequisites:
```
Java SE Development Kit 8 or above
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
