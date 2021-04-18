# Virtual Machine Contracts

Semux utilizes the Ethereum VM smart contract language, so porting your favorite Ethereum contracts is a breeze!
You can also use your favorite Ethereum tools to interact with the Semux blockchain like https://remix.ethereum.org/

## Writing a contract
You should consider using [Solidity](https://solidity.readthedocs.io/en/v0.5.3/) when writing smart contracts for 
legibility and maintainability sake.  It offers an object-oriented language that compiles down to Ethereum VM language.

Let's start with a hello world program.

```
pragma solidity ^0.4.22;

contract helloWorld {
    
    string name;
    
    constructor(string _name) public {
        name = _name;
    }
    
    function hello () public pure returns (string) {
        return 'Hello World!';
    }
}
```

## Compiling the contract
Use a tool like https://remix.ethereum.org/ to compile your solidity into bytecode

* Select the compiler that matches your version.
* Select the EVM Version `constantinople`
* Click `Compile`

You will get the following bytecode and ABI
```json
{
	"linkReferences": {},
	"object": "608060405234801561001057600080fd5b50604051610243380380610243833981018060405281019080805182019291905050508060009080519060200190610049929190610050565b50506100f5565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061009157805160ff19168380011785556100bf565b828001600101855582156100bf579182015b828111156100be5782518255916020019190600101906100a3565b5b5090506100cc91906100d0565b5090565b6100f291905b808211156100ee5760008160009055506001016100d6565b5090565b90565b61013f806101046000396000f300608060405260043610610041576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff16806319ff1d2114610046575b600080fd5b34801561005257600080fd5b5061005b6100d6565b6040518080602001828103825283818151815260200191508051906020019080838360005b8381101561009b578082015181840152602081019050610080565b50505050905090810190601f1680156100c85780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b60606040805190810160405280600c81526020017f48656c6c6f20576f726c642100000000000000000000000000000000000000008152509050905600a165627a7a7230582064c7ebdb910291bade9ddfa02d92001c3cbddef2723329d051a10663799dfff00029",
	"opcodes": "PUSH1 0x80 PUSH1 0x40 MSTORE CALLVALUE DUP1 ISZERO PUSH2 0x10 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST POP PUSH1 0x40 MLOAD PUSH2 0x243 CODESIZE SUB DUP1 PUSH2 0x243 DUP4 CODECOPY DUP2 ADD DUP1 PUSH1 0x40 MSTORE DUP2 ADD SWAP1 DUP1 DUP1 MLOAD DUP3 ADD SWAP3 SWAP2 SWAP1 POP POP POP DUP1 PUSH1 0x0 SWAP1 DUP1 MLOAD SWAP1 PUSH1 0x20 ADD SWAP1 PUSH2 0x49 SWAP3 SWAP2 SWAP1 PUSH2 0x50 JUMP JUMPDEST POP POP PUSH2 0xF5 JUMP JUMPDEST DUP3 DUP1 SLOAD PUSH1 0x1 DUP2 PUSH1 0x1 AND ISZERO PUSH2 0x100 MUL SUB AND PUSH1 0x2 SWAP1 DIV SWAP1 PUSH1 0x0 MSTORE PUSH1 0x20 PUSH1 0x0 KECCAK256 SWAP1 PUSH1 0x1F ADD PUSH1 0x20 SWAP1 DIV DUP2 ADD SWAP3 DUP3 PUSH1 0x1F LT PUSH2 0x91 JUMPI DUP1 MLOAD PUSH1 0xFF NOT AND DUP4 DUP1 ADD OR DUP6 SSTORE PUSH2 0xBF JUMP JUMPDEST DUP3 DUP1 ADD PUSH1 0x1 ADD DUP6 SSTORE DUP3 ISZERO PUSH2 0xBF JUMPI SWAP2 DUP3 ADD JUMPDEST DUP3 DUP2 GT ISZERO PUSH2 0xBE JUMPI DUP3 MLOAD DUP3 SSTORE SWAP2 PUSH1 0x20 ADD SWAP2 SWAP1 PUSH1 0x1 ADD SWAP1 PUSH2 0xA3 JUMP JUMPDEST JUMPDEST POP SWAP1 POP PUSH2 0xCC SWAP2 SWAP1 PUSH2 0xD0 JUMP JUMPDEST POP SWAP1 JUMP JUMPDEST PUSH2 0xF2 SWAP2 SWAP1 JUMPDEST DUP1 DUP3 GT ISZERO PUSH2 0xEE JUMPI PUSH1 0x0 DUP2 PUSH1 0x0 SWAP1 SSTORE POP PUSH1 0x1 ADD PUSH2 0xD6 JUMP JUMPDEST POP SWAP1 JUMP JUMPDEST SWAP1 JUMP JUMPDEST PUSH2 0x13F DUP1 PUSH2 0x104 PUSH1 0x0 CODECOPY PUSH1 0x0 RETURN STOP PUSH1 0x80 PUSH1 0x40 MSTORE PUSH1 0x4 CALLDATASIZE LT PUSH2 0x41 JUMPI PUSH1 0x0 CALLDATALOAD PUSH29 0x100000000000000000000000000000000000000000000000000000000 SWAP1 DIV PUSH4 0xFFFFFFFF AND DUP1 PUSH4 0x19FF1D21 EQ PUSH2 0x46 JUMPI JUMPDEST PUSH1 0x0 DUP1 REVERT JUMPDEST CALLVALUE DUP1 ISZERO PUSH2 0x52 JUMPI PUSH1 0x0 DUP1 REVERT JUMPDEST POP PUSH2 0x5B PUSH2 0xD6 JUMP JUMPDEST PUSH1 0x40 MLOAD DUP1 DUP1 PUSH1 0x20 ADD DUP3 DUP2 SUB DUP3 MSTORE DUP4 DUP2 DUP2 MLOAD DUP2 MSTORE PUSH1 0x20 ADD SWAP2 POP DUP1 MLOAD SWAP1 PUSH1 0x20 ADD SWAP1 DUP1 DUP4 DUP4 PUSH1 0x0 JUMPDEST DUP4 DUP2 LT ISZERO PUSH2 0x9B JUMPI DUP1 DUP3 ADD MLOAD DUP2 DUP5 ADD MSTORE PUSH1 0x20 DUP2 ADD SWAP1 POP PUSH2 0x80 JUMP JUMPDEST POP POP POP POP SWAP1 POP SWAP1 DUP2 ADD SWAP1 PUSH1 0x1F AND DUP1 ISZERO PUSH2 0xC8 JUMPI DUP1 DUP3 SUB DUP1 MLOAD PUSH1 0x1 DUP4 PUSH1 0x20 SUB PUSH2 0x100 EXP SUB NOT AND DUP2 MSTORE PUSH1 0x20 ADD SWAP2 POP JUMPDEST POP SWAP3 POP POP POP PUSH1 0x40 MLOAD DUP1 SWAP2 SUB SWAP1 RETURN JUMPDEST PUSH1 0x60 PUSH1 0x40 DUP1 MLOAD SWAP1 DUP2 ADD PUSH1 0x40 MSTORE DUP1 PUSH1 0xC DUP2 MSTORE PUSH1 0x20 ADD PUSH32 0x48656C6C6F20576F726C64210000000000000000000000000000000000000000 DUP2 MSTORE POP SWAP1 POP SWAP1 JUMP STOP LOG1 PUSH6 0x627A7A723058 KECCAK256 PUSH5 0xC7EBDB9102 SWAP2 0xba 0xde SWAP14 0xdf LOG0 0x2d SWAP3 STOP SHR EXTCODECOPY 0xbd 0xde CALLCODE PUSH19 0x3329D051A10663799DFFF00029000000000000 ",
	"sourceMap": "26:212:0:-;;;79:62;8:9:-1;5:2;;;30:1;27;20:12;5:2;79:62:0;;;;;;;;;;;;;;;;;;;;;;;;;;;;;129:5;122:4;:12;;;;;;;;;;;;:::i;:::-;;79:62;26:212;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;:::i;:::-;;;:::o;:::-;;;;;;;;;;;;;;;;;;;;;;;;;;;:::o;:::-;;;;;;;"
}
```

```json
[
	{
		"constant": true,
		"inputs": [],
		"name": "hello",
		"outputs": [
			{
				"name": "",
				"type": "string"
			}
		],
		"payable": false,
		"stateMutability": "view",
		"type": "function"
	},
	{
		"inputs": [
			{
				"name": "_name",
				"type": "string"
			}
		],
		"payable": false,
		"stateMutability": "nonpayable",
		"type": "constructor"
	}
]
```

## Deploying it!

To deploy a smart contract, you need to figure out three inputs:
- The contract code, which is the `object` attribute of the bytecode JSON file above.
- The initialization arguments (skip if there is no arguments)
    * Visit https://abi.hashex.org/
    * Paste the contract's ABI
    * Select function type `constructor` and provide the arguments
    * Copy the ABI-encoded output
- The gasLimit to provide (use the default `2000000` or more; the unused gas will be refunded)

Then, you can deploy a contract via the Semux API and Semux Core wallet:
- Select transaction type: `Deploy a contract`
- Enter the value, gas limit and gas price based on your use case
- Paste the **contract code** and **ABI-encoded initialization arguments** into the data input box.
- Click `Send`

Once the transaction is processed sucessfully, a smart contract will be created. To get its address, you can:
- Check the transaction result in Semux Core wallet.
- **OR** visit https://www.semux.info/explorer.
- **OR** compute the address programmatically.

## Calling the deployed contract
 
To call a method of the deployed contract, you need to figure out what data to pass to it.

1. Past the contract's ABI to https://abi.hashex.org/
2. Select the method you'd like to call into and provide the arguments
3. Copy the ABI-encode output, which will be used as the "Data" for your transaction.

Then, switch to your Semux Core wallet or API explorer for sending transaction.

## Links
- Block explorer: https://semux.info/explorer
- Remix IDE: https://remix.ethereum.org
- ABI tool: https://abi.hashex.org/
- Ethereum Development Tutorial: https://github.com/ethereum/wiki/wiki/Ethereum-Development-Tutorial