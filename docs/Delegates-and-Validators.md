# Delegates and Validators

### What is a delegate

**Delegates** are special accounts on the **Semux BFT** consensus. Delegates are accounts that are available for voting and could become a validator. To register as a delegate you need to have `1000 SEMs` + transaction fee.

### How to register as a delegate

**To become a delegate**
1. Make sure you have enough balance (1000 SEMs + transaction fee);
2. Click on the `Delegates` tab;
3. In the middle right side you can see a bar **below** `Unvote`;
4. Type the **name** you want your delegate to appear;
5. Click `Register as delegate`.

### What is validator

**Validators** are delegates who are allowed to forge/mine blocks and validate transaction for the BFT Protocol. To become a `Validator`, a `Delegate` need to have enough number of votes to make it to the `Top 100` of the list. **Validators** are indicated with the **V** symbol compared to **S** symbol for other delegates.

### How to become a validator

**To become a validator**
1. Make sure to register as delegate
2. Vote for your own delegate
    1. Click on the `Delegates` tab;
    2. On the right side type the number of votes you want to put for your delegate;
    3. Click on your delegate;
    4. Click Vote (note: votes will remain locked until you `unvote`).
3. Wait for others to vote for your own delegate

### Number of validators

At the start of the network there will be **16 validators slots**. The `Top 16 validators` with the most number of votes will automatically become validators. Delegates can add more votes to remain in the `Top 16`. The number of validators will increment by **1 every 2 hours** until the **maximum of 100 validators** is reached.

### Recommended validator setup

Validator needs to be backed by a powerful computer.

**Minimum Setup**
* 8GB Memory
* Dual Core CPU
* 100 Mbps Bandwidth

**Recommended Setup**
* 16GB Memory
* Quad Core CPU
* 200 Mbps Bandwidth

Note: bandwidth requirements are for both inbound and outbound traffic.
