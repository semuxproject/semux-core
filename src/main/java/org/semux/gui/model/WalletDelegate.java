package org.semux.gui.model;

import org.semux.core.state.Delegate;

public class WalletDelegate extends Delegate {

    protected long votesFromMe;

    protected long numberOfBlocksForged;
    protected long numberOfTurnsHit;
    protected long numberOfTurnsMissed;

    public WalletDelegate(Delegate d) {
        super(d.getAddress(), d.getName(), d.getRegisteredAt(), d.getVotes());
    }

    public long getVotesFromMe() {
        return votesFromMe;
    }

    public void setVotesFromMe(long votesFromMe) {
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
}
