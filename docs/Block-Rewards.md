### Total supply

The max supply of semux is capped at 100,000,000 SEM.

| Purpose       | Amount         | Note                                                       |
|---------------|----------------|------------------------------------------------------------|
| Block Rewards | 75,000,000 SEM | Distributed to Semux validators                            |
| Foundation    | 10,000,000 SEM | Development, marketing, promotion and bounties             |
| Community     | 10,000,000 SEM | Alpha/beta/rc test, bitcointalk airdrop, BTC giveway, etc. |
| Founder       | 5,000,000 SEM  | Founder and core developers of Semux                       |

### Block reward

Semux are created each time a validator forges a new block.

```java
public static long getBlockReward(long number) {
    if (number <= 25_000_000) {
        return 3 * Unit.SEM;
    } else {
        return 0;
    }
}
```
