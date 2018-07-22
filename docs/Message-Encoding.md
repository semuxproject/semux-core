# Message Encoding
Semux network messages format.

## Signatures
A signed message takes the message bytes and signs it, then appends this signature to the message.

## Types

### boolean
1 bit

### long
8 bytes

### int
4 bytes 

### byte[]
First write the number of bytes using VLQ, then write each byte.

### string
First write the number of bytes using VLQ, then write each byte of the string.

## VLQ
Variable length quantity.  This encoding allows for writing both large and small numbers in an efficient manner.

