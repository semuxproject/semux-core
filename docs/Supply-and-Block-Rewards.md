# Block Rewards

Semux is a blockchain platform powered by 'Semux BFT' consensus algorithm.

Its native cryptocurrency is called **SEM**.

SEM is a fundamental asset for operation of Semux platform, which thereby provides a public distributed ledger for 
transactions. SEM is used to pay for transaction fees and as a fuel for running decentralised apps. Another crucial use 
case for SEM is the consensus algorithm. SEM is used for voting on [Validators](./Delegates-and-Validators.md) who forge 
new blocks and verify the transactions.

### Total Supply

The max supply of SEM cryptocurrency is capped at 32,000,000 SEM.

| Purpose       | Amount         | Note                                                                |
|---------------|----------------|---------------------------------------------------------------------|
| Block Rewards | 22,000,000 SEM | Block rewards to Semux Network validators                          |
| Community     |  5,000,000 SEM | Distributed to airdrop and testing program participants             |
| Development   |  4,000,000 SEM | Reserved for development, marketing and promotion                   |
| Founders      |  1,000,000 SEM | Retained by the founders who developed the Semux technology         |

Notes:
- The numbers listed above are post-burning amount.
- 100% of the remaining fund has been moved to a multi-sig wallet. See [this doc](./Multi-Sig-Wallet.md) for more details.

### Block reward

New SEMs are created each time a validator forges a new block.

Once maximum supply is reached, new SEM will no longer be created.

Validators receive the block reward as well as all fees contained in the block.

```
Block 0 - 2,000,000          : 3 SEM
Block 2,000,001 - 6,000,000  : 2 SEM
Block 6,000,001 - 14,000,000 : 1 SEM
Block 14,000,001+            : 0 SEM
```
After the 14th million blocks, Semux will transition to a fee market.

### Real-time API

The following real-time APIs about SEM stats have been provided by api.semux.org.

* [`/max-supply`](http://api.semux.org/max-supply): The best approximation of the maximum amount of coins that will ever exist in the lifetime of the cryptocurrency.
* [`/total-supply`](http://api.semux.org/total-supply): The total amount of coins in existence right now (minus any coins that have been verifiably burned).
* [`/circulating-supply`](http://api.semux.org/circulating-supply): The best approximation of the number of coins that are circulating in the market and in the general public's hands.
