# Semux BFT Consensus

## What is BFT

In fault-tolerant computer systems, and in particular distributed computing systems, Byzantine fault tolerance (BFT) is 
the characteristic of a system that tolerates the class of failures known as the 
[Byzantine Generals' Problem](https://en.wikipedia.org/wiki/Byzantine_fault_tolerance).

## What is Semux BFT

For each block, one validator is chosen to propose a block.  This block is then sent to other validators to validate.  
If more than 2/3 of validators agree that the block is valid, the block is accepted.

## Why BFT consensus is preferred

In a distributed, trustless system, a 2/3 majority of votes agreeing is sufficient to have high confidence that the 
state of the system is valid.

dPOS is preferable for energy use and scalability.

## Semux BFT specifications

There are six phases to Semux BFT Consensus.  On each block, the validators will go through these phases to forge the 
next block.

### 1 - New Height

**Time: 3 seconds (firm)**

Set height to `lastHeight + 1`

Send new height message to all peers.

### 2 - Propose

**Time: 12 seconds (firm)**

If there are rejected votes, clear them, and increment view.

If you are the primary validator, propose a block, and broadcast it.

Send new view message to all peers.

### 3 - Validate

**Time: 6 seconds (firm)**

If you have received a proposal, validate it, and vote on results.

If you did not receive a proposal, vote no.

Send validate vote to all peers.

### 4 - PreCommit

**Time: 6 seconds (firm)**

Check if there is 2/3 validators approved votes, if so vote yes.

Else vote no.

Send preCommit vote to all peers.

**On failure: go back to propose**

### 5 - Commit

**Time: 3 seconds (or less)**

Send message confirming receiving 2/3 preCommit votes (finalized certificate).

Enter finalize phase upon time out or receiving 2/3 commit votes, whichever comes first.

This phase is mainly for synchronization purpose.

### 6 - Finalize

**Time: 3 seconds (firm)**

Check precommit votes again, just to be sure.

Write the block into database and update global state.

**On Success: go to new height**
