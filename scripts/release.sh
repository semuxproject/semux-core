#!/bin/sh

name=semux-1.0.1

# change work directory
cd "$(dirname "$0")/../"

# build
mvn clean && mvn install || exit

# archive
mv dist $name
tar -czvf $name.tar.gz $name || exit
zip -r $name.zip $name || exit

# clean
rm -fr $name

/**
 * Get block reward for the given block.
 *
 * @param number
 *            block number
 * @return the block reward
 */
public static long getBlockReward(long number) {
    if (number <= 1_000_000) {
        return 100 * Unit.SEM;
    } else if (number <= 3_000_000) {
        return 50 * Unit.SEM;
    } else if (number <= 7_000_000) {
        return 25 * Unit.SEM;
    } else {
        return 0;
    }
}
