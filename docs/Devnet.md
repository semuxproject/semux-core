# Devnet 

Devnet is a feature similiar to [Bitcoin's Regtest Mode](https://bitcoin.org/en/glossary/regression-test-mode) that 
allows developers to run a local blockchain with a single Semux node for testing purpose.

To start a devnet, run:

```bash
./semux-cli.sh --network devnet
```

### Devnet Validator

There is a default validator that allows you to forge blocks on Devnet:

- Public Key = `0x23a6049381fd2cfb0661d9de206613b83d53d7df`
- Private Key = `0x302e020100300506032b657004220420acbd5f2cb2b6053f704376d12df99f2aa163d267a755c7f1d9fe55d2a2dc5405`

To start forging blocks on Devnet, run:
```bash
./semux-cli.sh --importprivatekey 302e020100300506032b657004220420acbd5f2cb2b6053f704376d12df99f2aa163d267a755c7f1d9fe55d2a2dc5405
```
then specify `0x23a6049381fd2cfb0661d9de206613b83d53d7df` as your coinbase.

### Testnet

If you prefer using a public testing network, see [Testnet](./Testnet.md).
