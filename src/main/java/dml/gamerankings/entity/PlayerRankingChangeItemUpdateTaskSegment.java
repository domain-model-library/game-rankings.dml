package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;


public class PlayerRankingChangeItemUpdateTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<RankItem> rankItemList;

    public PlayerRankingChangeItemUpdateTaskSegment() {
    }

    public PlayerRankingChangeItemUpdateTaskSegment(String id) {
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
