package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;

/**
 * PlayerRank 批量更新任务段实体。
 * 包含一批需要更新的排名信息（纯 playerId + rank，不承载 PlayerRank 实体）。
 */
public class PlayerRankUpdateTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<RankItem> rankItemList;

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

    public List<RankItem> getRankItemList() {
        return rankItemList;
    }

    public void setRankItemList(List<RankItem> rankItemList) {
        this.rankItemList = rankItemList;
    }
}
