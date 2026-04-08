package dml.gamerankings.entity;

public class PlayerRankingChangeItem {
    private String playerId;
    private int lastRank;
    private int currentRank;

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public int getLastRank() {
        return lastRank;
    }

    public void setLastRank(int lastRank) {
        this.lastRank = lastRank;
    }

    public int getCurrentRank() {
        return currentRank;
    }

    public void setCurrentRank(int currentRank) {
        this.currentRank = currentRank;
    }

    public void update(int rank) {
        lastRank = currentRank;
        currentRank = rank;
    }

    public void reset() {
        lastRank = 0;
        currentRank = 0;
    }
}
