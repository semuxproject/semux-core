# Change Log

## [v2.0.0](https://github.com/semuxproject/semux-core/tree/v2.0.0) (2019-08-25)

This release features the **virtual machine hardfork** and **HD wallet**.

**Important notice**

Please backup your wallet file, `wallet.data`, and do a full-sync with the network. Note that
the configuration file has also been updated; suggest using the new one.

**Bug fixes:**

- Fixed a capacity codec bug in P2P handshake (#214)
- Fixed the invalid transaction results in database issue (#221)
- Fixed various EVM integration issues (#182, #183, #184, #190, #209, #210, #224, #229)

**Improvements:**

- Updated docs (#156, #158, #193)
- Reset the `testnet` with new keys (#175, #176)
- Packed JVM images in releases (#225)
- Added support for quotes in console dialog (#203)

**New features:**

- Consensus
    - Replaced the block size limit with a `20m` gas limit (#211, #213, #214)
- Sync
    - Added support for the experimental `FAST_SYNC` protocol (#155, #228, #232)
- Wallet
    - Added support for HD wallet (#173, #174, #231)
- API
    - Bumped the version to `v2.3.0`
    - Added `gas` and `gasPrice` in the `TransactionType` response (#226)
    - Added the `InternalTransactionType` (#188)
    - Updated the `/trasaction/create` and `/trasaction/call` endpoints (#194)
    - Updated the `/compose-raw-transaction` endpoint (#195)
    - Updated the `/transaciton-result` endpoint (#219)
- GUI
    - Added support for quotes in console dialog (#203)


## [v1.4.0](https://github.com/semuxproject/semux-core/tree/v1.4.0) (2019-04-22)

This release includes incremental improvements and bugfixes since last version. Major changes
are the block rewards adjustment and virtual machine implementation (in place but **not activated**).

**Bugfixes:**
- Fix the 2/3 BFT quorum size rounding error (#134, #142)
- Start syncing when the number of connections is low (#130)

**Improvements:**
- Fast block validation using batch validation (#150)
- Add `aarch64` native support (#89, #117)
- Suggest use OpenJDK (#131)
- Update error messages and descriptions (#81, #79, #94, #103)
- Add empty password shortcut (#108)
- Update dependent libraries (#79, #141)
- Limit the number of validators on testnet (#123)

**Features:**

- Consensus
    - Update the block reward function (#151)
- VM
    - Introduce the VM fork signal (#67, #137, #139)
    - Refactor transaction results (#77, #149)
    - VM tests (#90, #97, #113, #114, #117, #112, #129, #140)
- Wallet
    - HD wallet tests (#133, #132, #138)
- API
    - Bump version to v2.2.0
    - Remove `blockNumber` from `*TransactionType`
    - Add `/transaction-result` endpoint for transaction result
- P2P
    - Upgrade protocol to support light client (#146)

## [v1.3.0](https://github.com/semuxproject/semux-core/tree/v1.3.0) (2018-08-05)

This release fix the validator timestamp issue and introduces fast syncing.

**Bugfixes:**
- GUI
    - Fixed the sender address order issue
- Tools
    - Fixed windows unicode directories 

**Features and enhancements:**
- Consensus
    - Changed the creation of block proposal timestamp
    - Introduced fast syncing
- Core
    - Introduced NTP time adjustment
    - Removed 32-bit system support
- API
    - Removed API v1
- Net
    - Added filter of duplicated transactions


## [v1.2.0](https://github.com/semuxproject/semux-core/tree/v1.2.0) (2018-05-25)

This release introduces Java 10 support plus a few API & documentation improvements for third-party service integration & light wallet implementation.

**Fixed bugs:**

- Net
  - Fixed a memory leak caused by connection limiter
- API 2.0.0
  - Fixed a bug that `data` parameter was marked as required for making transactions in API v2.0.0 Swagger definition
- Consensus
  - Fixed an issue that SemuxBFT reports a wrong fork activation height for a freshly synced client
- GUI
  - Fixed an issue that long aliases can break rendering

**New features:**

- Add Java 10 Support
- Add API 2.1.0 based off API 2.0.0
  - Add `DELETE /account?address`
  - Add `GET /account/votes?address` API
  - Add `GET /account/pending-transactions?address&from&to` API
  - Add `validator` flag to `DelegateType`
  - Add `network` and `capabilities` into the response of `GET /info` API
  - Add an optional parameter `privateKey` to `POST /account` that enables consumers to import private keys
  - Add optional parameters `nonce` and `validateNonce` to transaction ops that enables consumers to manage transaction nonces on client-side
  - Validate raw transaction passing in `POST /transaction/raw`
  - Change `fee` parameter from required to optional, default to minimum fee if omitted
- GUI
  - Add a transaction filter on Transactions panel
  - Add dropdown for selecting recipient on Send panel
- Consensus
  - Add blockchain checkpoints
- Security
  - Provide safe ways for automatic wallet unlock to address an issue that `--password` CLI option exposes wallet password to process explorer
    - environment variable: `SEMUX_WALLET_PASSWORD`

**Implemented enhancements:**

- GUI
  - Rearrange sorting of delegate panel
    - Reflect internal validator positions within 200-block round
    - Prioritize registration block over delegate name
- Security
  - Don't dump private key in log file on create
- Docs
  - Add devnet doc and API base unit doc
  - Add links to API clients
  - Add links to delegate pools and block explorers
  - Improve API descriptions and validation patterns in swagger definition
  - Re-organize documentation

## [v1.1.1](https://github.com/semuxproject/semux-core/tree/v1.1.1) (2018-04-22)
[Full Changelog](https://github.com/semuxproject/semux-core/compare/v1.1.0...v1.1.1)

**Fixed bugs:**

- Fix a bug that database folders are wrongly named under Turkish Windows

**Implemented enhancements:**

- Provide a consistent progressbar on OSX with Windows and Linux
- Validate network name before startup
- Optimize client version lookup
- Change Windows Java download URL from java.com to oracle.com

## [v1.1.0](https://github.com/semuxproject/semux-core/tree/v1.1.0) (2018-04-15)
[Full Changelog](https://github.com/semuxproject/semux-core/compare/v1.0.1...v1.1.0)

**UNIFORM_DISTRIBUTION Fork**

`UNIFORM_DISTRIBUTION` fork is introduced in v1.1.0 to address issue

Validators are strongly suggested to upgrade to v1.1.0 ASAP in order to participate in the activation of `UNIFORM_DISTRIBUTION` fork which will be activated as soon as there are 1500 out of past 2000 blocks signaling the fork.

Please note your validator node will stop forging blocks if you are unable to install v1.1.0 in advance of fork activation.

**Database Upgrade**

v1.1.0 will automatically apply a local database upgrade which takes about 1 ~ 5 minutes during wallet startup. Please ensure that you have at least 3 GB of space left on your disk.

**Fixed bugs:**

- GUI: Fix model refresh delay
- Consensus: Fix sync votes validation issue

**Implemented enhancements:**

- Core: Change default POSIX permissions of wallet.data and config files to 600
- Docs: Basic introductions to Semux BFT consensus
- GUI: Inform user when a new version of Semux Wallet has been posted
- Docs: Move API documentation to https://semuxproject.github.io/semux-api-docs/
- API: Add API v2.0.0 and Swagger UI
- Crypto: Crypto function speed is improved ~70% by introducing libsodium & ripemd160 native implementation
- DB: Separate database from different network
- Util: Standardize system exit code
- Docs: Move wiki into the main repo to accept PRs on docs
- GUI: Add splash screen
- Consensus: Add memoization to Vote\#validate to avoid repeated validations
- GUI: Add getBlockByNumber to console
- Core: Introduce `Amount` class to normalize units in source code
- GUI: Use BigDecimal instead of double for correctness
- GUI: Refactor status bar

**Testnet changes:**

- Increase block size limit to 10 MB

**Deprecations:**

- API v1.0.1 is deprecated and planned to be removed in a future release

## [v1.0.1](https://github.com/semuxproject/semux-core/tree/v1.0.1) (2018-03-06)
[Full Changelog](https://github.com/semuxproject/semux-core/compare/v1.0.0...v1.0.1)

**Fixed bugs:**

- A validator node might stuck in sync process
    - Consensus: Don't sync when a validator is in FINALIZE state
    - Consensus: Fix unconditional wait of SemuxSync\#isRunning
- API: Fix typos in API docs
- GUI: Dispose address book dialog when the wallet GUI is locked
- GUI: Import wallet imports addressbook too
- GUI: Focus text field on right click
- Net: Properly separate mainnet and testnet
- CLI: Flush async loggers in `Launcher` class

**Implemented enhancements:**

- Add Java 9 Support
- GUI: Support customized based unit and show full digits by default
- GUI: Validate address alias length
- GUI: Clean up address label
- GUI: Update to new logo
- GUI: Render to highest precision
- GUI: Add Mnemonic Keys to the UI
- GUI: Added feedback for empty names on address book entries
- GUI: Add address book edit dialog
- GUI: Add InputDialog to Windows TaskBar
- GUI: Provide detailed tooltip for Data input
- GUI: Add prefix to address in generated QR Code
- GUI: Add a Title to Rename Account Dialog
- GUI: Add a Title to Import Dialog
- GUI: Add Semux Logo to About Dialog
- GUI: Add command console
- Consensus: Optimize transaction validation
- Config: Disallow default API username or password
- Net: Shuffle the list of nodes returned from GET\_NODES message in order to balance the load on nodes
- Net: Add mainnet.semux.net as an alternative dns seed
- Net, Config: Allow for additional DNS seeds
- Core: Upgrade Leveldb From 1.8 to 1.18
- Core: Improve error reporting of UNVOTE transaction
- Core: Optimize wallet lookup
- API: Update error messages to be consistent
- API: Validate `hash` on `getBlock` calls
- API: Add a parameter 'name' to `/create_account`
- API: Add parameter descriptions
- API: Consistent error handling
- API: Pretty print API response when get parameter pretty=true
- API: Add sign/verify messages calls
- API: Add a data field `transactionCount` to the response of `/get_account` API
- API: Add data field TransactionType\#blockNumber
- Tools: Upgrade Jackson to 2.9.4
- Windows: Detect Installation of Microsoft Visual C++ Redistributable Package Under Windows Platform
