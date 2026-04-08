import dml.gamerankings.entity.PlayerRankingItem;

public class TestPlayerRankingItem implements PlayerRankingItem {
    private long playerId;
    private long metricValue;
    private int rank;

    @Override
    public void setPlayerId(Object playerId) {
        this.playerId = (Long) playerId;
    }

    @Override
    public void setMetricValue(long metricValue) {
        this.metricValue = metricValue;
    }

    public Object getPlayerId() {
        return playerId;
    }

    @Override
    public long getMetricValue() {
        return metricValue;
    }

    @Override
    public void setRank(int rank) {
        this.rank = rank;
    }

    @Override
    public int getRank() {
        return rank;
    }
}
