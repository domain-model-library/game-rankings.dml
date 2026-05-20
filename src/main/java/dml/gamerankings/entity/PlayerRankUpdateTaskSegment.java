package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;

/**
 * PlayerRank 批量更新任务段实体。
 * 包含一批需要更新的 PlayerRank 记录。
 */
public class PlayerRankUpdateTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<PlayerRank> playerRankList;

    public PlayerRankUpdateTaskSegment() {
    }

    public PlayerRankUpdateTaskSegment(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<PlayerRank> getPlayerRankList() {
        return playerRankList;
    }

    public void setPlayerRankList(List<PlayerRank> playerRankList) {
        this.playerRankList = playerRankList;
    }
}
