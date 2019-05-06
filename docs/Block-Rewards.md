# Block Rewards

Semux is a blockchain platform powered by 'Semux BFT' consensus algorithm.

It's native cryptocurrency called **SEM**.

SEM is a fundamental asset for operation of Semux platform, which thereby provides a public distributed ledger for transactions. SEM is used to pay for transaction fees and as a fuel for running decentralised apps. Another crucial use case for SEM is the consensus algorithm. SEM is used for voting on Validators who forge new blocks and verify the transactions.

### Block reward

New SEM coins are created each time a validator forges a new block.  

Once maximum supply is reached, new SEM will no longer be created.

Validators receive the block reward as well as all fees contained in the block.

```
Block 0 - 2,000,000          : 3 SEM
Block 2,000,001 - 6,000,000  : 2 SEM
Block 6,000,001 - 14,000,000 : 1 SEM
Block 14,000,001+            : 0 SEM
```
After the 14th million blocks, Semux will transition to a fee market.

### Supply

The max supply of SEM cryptocurrency is capped at 32,000,000 SEM.

| Purpose       | Amount         | Note                                                                |
|---------------|----------------|---------------------------------------------------------------------|
| Block Rewards | 22,000,000 SEM | To be distributed to Semux validators                               |
| Foundation    | 5,000,000 SEM  | Development, marketing, promotion and bounties                      |
| Community     | 5,000,000 SEM  | Have been distributed to the community: Alpha/Beta/RC test participants, bitcointalk/BTC/ETH aidrdrop, etc. |


### Real-time API

- Circulating Supply: http://api.semux.org/circulating-supply
- Total Supply: http://api.semux.org/total-supply
- Max Supply: http://api.semux.org/max-supply
