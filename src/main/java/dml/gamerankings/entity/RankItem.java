package dml.gamerankings.entity;

/**
 * 排名项值对象。
 * 纯数据描述：哪个玩家排名多少，不绑定任何特定场景（榜单、任务段等均可使用）。
 */
public class RankItem {
    private Object playerId;
    private int rank;

    public RankItem() {
    }

    public RankItem(Object playerId, int rank) {
        this.playerId = playerId;
        this.rank = rank;
    }

    public Object getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Object playerId) {
        this.playerId = playerId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
