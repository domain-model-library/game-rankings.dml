package dml.gamerankings.entity;

public interface PlayerRank {
    void setPlayerId(Object playerId);

    Object getPlayerId();

    void setRank(int rank);

    int getRank();
}
