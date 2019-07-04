# Virtual Machine Contracts

Semux utilizes the Ethereum VM smart contract language, so porting your favorite Ethereum contracts is a breeze!
You can also use your favorite Ethereum tools to interact with the Semux blockchain like [Remix](https://remix.ethereum.org/)

## Writing a contract
You should consider using [Solidity](https://solidity.readthedocs.io/en/v0.5.3/) when writing smart contracts for 
legibility and maintainability sake.  It offers an object oriented language that compiles down to Ethereum VM language.

Let's start with a hello world program.

```
pragma solidity ^0.4.22;

contract helloWorld {
    function hello () public pure returns (string) {
        return 'Hello World!';
    }
}
```

## Compiling a contract
Use a tool like [Remix](https://remix.ethereum.org/) to compile your solidity into bytecode

* Select the compiler that matches your version.
* Select the EVM Version `byzantium`
* Click `Compile`
* Click `bytecode`

You will end up with something like the following
```json
{
	"linkReferences": {},
	"object": "608060405234801561001057600080fd5b5061013f806100206000396000f300608060405260043610610041576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806319ff1d2114610046575b600080fd5b34801561005257600080fd5b5061005b6100d6565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561009b578082015181840152602081019050610080565b50505050905090810190601f1680156100c85780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b60606040805190810160405280600c81526020017f48656c6c6f20576f726c642100000000000000000000000000000000000000008152509050905600a165627a7a723058208a3a6a0db56feab0c73c174116a4356dfe91c1efc802f294396c02be417477980029",
	"opcodes": "PUSH1 0x80 PUSH1 0x40 MSTORE CALLVALUE DUP1 ISZERO PUSH2 0x10 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST POP PUSH2 0x13F DUP1 PUSH2 0x20 PUSH1 0x0 CODECOPY PUSH1 0x0 RETURN STOP PUSH1 0x80 PUSH1 0x40 MSTORE PUSH1 0x4 CALLDATASIZE LT PUSH2 0x41 JUMPI PUSH1 0x0 CALLDATALOAD PUSH29 0x100000000000000000000000000000000000000000000000000000000 SWAP1 DIV PUSH4 0xFFFFFFFF AND DUP1 PUSH4 0x19FF1D21 EQ PUSH2 0x46 JUMPI JUMPDEST PUSH1 0x0 DUP1 REVERT JUMPDEST CALLVALUE DUP1 ISZERO PUSH2 0x52 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST POP PUSH2 0x5B PUSH2 0xD6 JUMP JUMPDEST PUSH1 0x40 MLOAD DUP1 DUP1 PUSH1 0x20 ADD DUP3 DUP2 SUB DUP3 MSTORE DUP4 DUP2 DUP2 MLOAD DUP2 MSTORE PUSH1 0x20 ADD SWAP2 POP DUP1 MLOAD SWAP1 PUSH1 0x20 ADD SWAP1 DUP1 DUP4 DUP4 PUSH1 0x0 JUMPDEST DUP4 DUP2 LT ISZERO PUSH2 0x9B JUMPI DUP1 DUP3 ADD MLOAD DUP2 DUP5 ADD MSTORE PUSH1 0x20 DUP2 ADD SWAP1 POP PUSH2 0x80 JUMP JUMPDEST POP POP POP POP SWAP1 POP SWAP1 DUP2 ADD SWAP1 PUSH1 0x1F AND DUP1 ISZERO PUSH2 0xC8 JUMPI DUP1 DUP3 SUB DUP1 MLOAD PUSH1 0x1 DUP4 PUSH1 0x20 SUB PUSH2 0x100 EXP SUB NOT AND DUP2 MSTORE PUSH1 0x20 ADD SWAP2 POP JUMPDEST POP SWAP3 POP POP POP PUSH1 0x40 MLOAD DUP1 SWAP2 SUB SWAP1 RETURN JUMPDEST PUSH1 0x60 PUSH1 0x40 DUP1 MLOAD SWAP1 DUP2 ADD PUSH1 0x40 MSTORE DUP1 PUSH1 0xC DUP2 MSTORE PUSH1 0x20 ADD PUSH32 0x48656C6C6F20576F726C64210000000000000000000000000000000000000000 DUP2 MSTORE POP SWAP1 POP SWAP1 JUMP STOP LOG1 PUSH6 0x627A7A723058 KECCAK256 DUP11 GASPRICE PUSH11 0xDB56FEAB0C73C174116A4 CALLDATALOAD PUSH14 0xFE91C1EFC802F294396C02BE4174 PUSH24 0x980029000000000000000000000000000000000000000000 ",
	"sourceMap": "26:113:0:-;;;;8:9:-1;5:2;;;30:1;27;20:12;5:2;26:113:0;;;;;;;"
}
```

## Calling semux to create contract

If you take the object value above and call semux create() with that as data, supplying sufficient gas (>136363), it will
create the contract.

You now need to determine what your contract address is.  There are programmatic ways to determine it, but if you
find the contract in your Transactions window, you can see something like ```To: 0xd2b17f8f2576e1e0a44656924140467591eb1a7b```
This is your contract address.


## Calling the contract

Since we made this an on-chain call, you will need to expend gas to call this contract, though you could make it a local call.


If you call the contract with the ```data``` field specifying your method signature, and parameters.
Supply the method name, along with method parameter types, keccak256 the result, and take the first 4 bytes of the 
result.  Then append your parameters (in this case, there are no parameters);

```java
byte[] method = HashUtil.keccak256("hello()".getBytes(StandardCharsets.UTF_8));
byte[] methodData = ByteArrayUtil.merge(Arrays.copyOf(method, 4),DataWord.ZERO.getData());

```

Now use your client to call ```call()```

```java
String txId = client.call(from, contractAddress, 1, 136363, methodData,0, false);
```

, you will get a transaction hash.  Wait until the contract has completed and retrieve the 
transactionResult, you will see

```json
{
  "success" : true,
  "message" : "successful operation",
  "result" : {
    "blockNumber" : "393",
    "code" : "SUCCESS",
    "logs" : [ ],
    "returnData" : "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000c48656c6c6f20576f726c64210000000000000000000000000000000000000000",
    "gas" : "10000000",
    "gasPrice" : "1",
    "gasUsed" : "22005",
    "fee" : "22005",
    "internalTransactions" : [ ]
  }
}
```

## Parse the return data
TODO

But basically ```c48656c6c6f20576f726c6421``` converted from Hex to string is ```Hello World!``` as we expect!




## Links
- Ethereum Development Tutorial: https://github.com/ethereum/wiki/wiki/Ethereum-Development-Tutorial