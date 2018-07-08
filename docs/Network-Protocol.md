# Network Protocol

Clients send messages to peers in order to interact with the blockchain.  These can be
divided into general peer messages, and messages that Validators use to forge new blocks.

Messages are written using the Semux [Message Encoding](./Message-Encoding.md) format.

## Standard Message Objects

### Peer

Message:
- ip (string)
- port (int)
- networkVersion (short)
- clientId (string)
- peerId (string)
- latestBlockNumber (long) 
- capabilities size (int)
- capabilities[] (string[])

## Peer Messages

### Hello

Code: 0x01

Initiate a connection with a peer.  No messages will be accepted until after this handshake is done.

Message:
- peer (Peer)
- timestamp (long)
- signature (byte[])

### World

Code: 0x02

Respond to a Hello message indicating successful connection.

Message:
- peer (Peer)
- timestamp (long)
- signature (byte[])

### Ping

Code: 0x03

Request a reply from the peer to ensure connection is still valid.

Message:
- timestamp (long)

### Pong

Code: 0x04

Respond to a Ping message.

Message:
- timestamp (long)

### Disconnect

Code: 0x00

Inform a peer of disconnection.  Reasons can include too many peers, invalid handshake, or other.

Message:
- reason (byte)

### Get Nodes

Code: 0x05

Request peers of the peer.

Message:
None.

### Nodes Response

Code: 0x06

Send a list of current peers.

Message:
- size (int) (number of nodes)
- ip (string)
- port (int)

### Transaction

Code: 0x07

Propagate a transaction from mempool.

Message:
- hash (byte[])
- networkId (byte)
- type (byte)
- to (byte[])
- amount (long)
- fee (long)
- nonce (long)
- timestamp (long)
- data (byte[])
- signature (byte[])


### Get Block Header

Code: 0x30

Request a block header.

Message:
- number (long)

### Block Header Response

Code: 0x31

Send a block header.

Message:
- hash (byte[])
- number (long)
- coinbase (byte[])
- previous hash (byte[])
- timestamp (long)
- transaction root (byte[])
- result root (byte[])
- state root (byte[])
- data (byte[])

## BFT Messages

These are messages passed as part of the [BFT](./Semux-BFT-Consensus.md)

### New Height

Code: 0x40

### New View

Code: 0x41

### Proposal

Code: 0x42

A block proposal.

### Vote

Code: 0x43

A vote on the current phase.

