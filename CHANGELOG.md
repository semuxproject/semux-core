# Change Log

## [v2.2.x](https://github.com/semuxproject/semux-core/tree/develop) (2020-xx-xx) (WIP)

This release adds support for EIP-665 precompiled contract.

NOTE:  A softfork `EIP665_PRECOMPILED_UPGRADE` is introduced.



## [v2.1.x](https://github.com/semuxproject/semux-core/tree/v2.1.1) (2019-09-10)

This release tries to fix several issues about the voting precompiled contracts. All nodes
are required to upgrade.

NOTE: A softfork `VOTING_PRECOMPILED_UPGRADE` is introduced.

**Bug fixes:**

- Fixed the traceability issue of vote/unvote calls (#249)
- Fixed the transaction result bug (#260)
- Fixed the local call bug (#257)

**New features:**
- API
    - Bumped the version to `v2.4.0`
    - Deprecated `POST /transaction/raw` (#267)
    - Deprecated `POST /account` and `DELETE /account` (#267)
    - Replaced `/call` with `/local-call` (#267)
    - Added `/local-create` (#267)
    - Added `/broadcast-raw-transaction` (#267)
    - Added `/create-account` and `/delete-account` (#267)
    - Added `/account/code` and `/account/storage` (#266)
- CLI
    - Added a database re-index tool (#262)


## [v2.0.x](https://github.com/semuxproject/semux-core/tree/v2.0.2) (2019-08-25)

This release features the **virtual machine hardfork** and **HD wallet**.

NOTE: A softfork `VIRTUAL_MACHINE` is introduced.

**Bug fixes:**
- Fixed a capacity codec bug in P2P handshake (#214)
- Fixed the invalid transaction results in database issue (#221)
- Fixed various EVM integration issues (#182, #183, #184, #190, #209, #210, #224, #229)

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

**Enhancements:**
- Updated docs (#156, #158, #193)
- Reset the `testnet` with new keys (#175, #176)
- Packed JVM images in releases (#225)
- Added support for quotes in console dialog (#203)


## [v1.4.x](https://github.com/semuxproject/semux-core/tree/v1.4.0) (2019-04-22)

This release includes incremental improvements and bugfixes since last version. Major changes
are the block rewards adjustment and virtual machine implementation (in place but **not activated**).

**Bug fixes:**
- Fix the 2/3 BFT quorum size rounding error (#134, #142)
- Start syncing when the number of connections is low (#130)

**New features:**
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

**Enhancements:**
- Fast block validation using batch validation (#150)
- Add `aarch64` native support (#89, #117)
- Suggest use OpenJDK (#131)
- Update error messages and descriptions (#81, #79, #94, #103)
- Add empty password shortcut (#108)
- Update dependent libraries (#79, #141)
- Limit the number of validators on testnet (#123)


## [v1.3.x](https://github.com/semuxproject/semux-core/tree/v1.3.0) (2018-08-05)

This release fix the validator timestamp issue and introduces fast syncing.

**Bug fixes:**
- GUI
    - Fixed the sender address order issue
- Tools
    - Fixed windows unicode directories 

**Enhancements:**
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


## [v1.2.x](https://github.com/semuxproject/semux-core/tree/v1.2.0) (2018-05-25)

This release introduces Java 10 support plus a few API & documentation improvements for third-party service integration & light wallet implementation.

**Bug fixes:**
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

**Enhancements:**
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


## [v1.1.x](https://github.com/semuxproject/semux-core/tree/v1.1.0) (2018-04-15)

This release contains bug fixes and enhancements.

A softfork `UNIFORM_DISTRIBUTION` is introduced.

**Bug fixes:**
- GUI: Fix model refresh delay
- Consensus: Fix sync votes validation issue

**Enhancements:**
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


## [v1.0.x](https://github.com/semuxproject/semux-core/tree/v1.0.1) (2018-03-06)

This release contains bug fixes and enhancements.

**Bug fixes:**
- A validator node might stuck in sync process
    - Consensus: Don't sync when a validator is in FINALIZE state
    - Consensus: Fix unconditional wait of SemuxSync\#isRunning
- API: Fix typos in API docs
- GUI: Dispose address book dialog when the wallet GUI is locked
- GUI: Import wallet imports addressbook too
- GUI: Focus text field on right click
- Net: Properly separate mainnet and testnet
- CLI: Flush async loggers in `Launcher` class

**Enhancements:**
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
