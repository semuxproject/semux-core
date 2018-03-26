/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.gui.model;

import static org.semux.core.Amount.ZERO;

import java.util.List;

import org.semux.Kernel;
import org.semux.core.Amount;
import org.semux.core.state.Delegate;
import org.semux.crypto.Hex;

public class WalletDelegate extends Delegate {

    protected Amount votesFromMe = ZERO;

    protected long numberOfBlocksForged;
    protected long numberOfTurnsHit;
    protected long numberOfTurnsMissed;

    public WalletDelegate(Delegate d) {
        super(d.getAddress(), d.getName(), d.getRegisteredAt(), d.getVotes());
    }

    public Amount getVotesFromMe() {
        return votesFromMe;
    }

    public void setVotesFromMe(Amount votesFromMe) {
        this.votesFromMe = votesFromMe;
    }

    public long getNumberOfBlocksForged() {
        return numberOfBlocksForged;
    }

    public void setNumberOfBlocksForged(long numberOfBlocksForged) {
        this.numberOfBlocksForged = numberOfBlocksForged;
    }

    public long getNumberOfTurnsHit() {
        return numberOfTurnsHit;
    }

    public void setNumberOfTurnsHit(long numberOfTurnsHit) {
        this.numberOfTurnsHit = numberOfTurnsHit;
    }

    public long getNumberOfTurnsMissed() {
        return numberOfTurnsMissed;
    }

    public void setNumberOfTurnsMissed(long numberOfTurnsMissed) {
        this.numberOfTurnsMissed = numberOfTurnsMissed;
    }

    public double getRate() {
        long total = numberOfTurnsHit + numberOfTurnsMissed;
        return total == 0 ? 0 : numberOfTurnsHit * 100.0 / total;
    }

    public boolean isValidator(Kernel kernel) {
        List<String> validators = kernel.getBlockchain().getValidators();
        for (String v : validators) {
            if (v.equals(Hex.encode(getAddress()))) {
                return true;
            }
        }
        return false;
    }
}
