import dml.gamerankings.entity.PlayerRank;

public class TestPlayerRank implements PlayerRank {
    private long playerId;
    private long metricValue;
    private int rank;

    @Override
    public void setPlayerId(Object playerId) {
        this.playerId = (Long) playerId;
    }

    public void setMetricValue(long metricValue) {
        this.metricValue = metricValue;
    }

    @Override
    public Object getPlayerId() {
        return playerId;
    }

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
