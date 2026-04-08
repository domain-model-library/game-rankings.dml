package dml.gamerankings.entity;

public interface PlayerRankingItem {
    void setPlayerId(Object playerId);

    Object getPlayerId();

    long getMetricValue();

    void setMetricValue(long metricValue);

    void setRank(int rank);

    int getRank();
}
