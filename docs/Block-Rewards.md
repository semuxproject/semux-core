# Block Rewards           

### Block reward

Semux are created each time a validator forges a new block.  

Once maximum supply is reached, new Semux will no longer be created.

Validators receive the block reward as well as all fees contained in the block.

```
Block        1 - 10000000 : 3 SEM
Block 10000001 - 25000000 : 2 SEM
Block 25000001 - 40000000 : 1 SEM
Block 40000001 -          : 0 SEM
```

### Supply

The max supply of semux is capped at 100,000,000 SEM.

| Purpose       | Amount         | Note                                                                |
|---------------|----------------|---------------------------------------------------------------------|
| Block Rewards | 75,000,000 SEM | Distributed to Semux validators                                     |
| Foundation    | 10,000,000 SEM | Development, marketing, promotion and bounties                      |
| Community     | 10,000,000 SEM | Alpha/Beta/RC test participants, bitcointalk/BTC/ETH aidrdrop, etc. |
| Founder       | 5,000,000 SEM  | Founders of Semux                                                   |


### Real-time API

- `Circulating Supply : `http://api.semux.org/circulating-supply 
- `Total Supply       : `http://api.semux.org/total-supply
- `Max Supply         : `http://api.semux.org/max-supply


NOTE: Circulating supply refers to all the available coins except the foundation funds (to be locked) and the community funds (to be distributed).
