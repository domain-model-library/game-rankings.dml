package dml.gamerankings.entity;

public class PlayerRankingItem {
    private Object playerId;
    private long metricValue;

    public PlayerRankingItem() {
    }

    public PlayerRankingItem(Object playerId, long metricValue) {
        this.playerId = playerId;
        this.metricValue = metricValue;
    }

    public Object getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Object playerId) {
        this.playerId = playerId;
    }

    public long getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(long metricValue) {
        this.metricValue = metricValue;
    }
}
