package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;


public class PlayerRankingChangeItemUpdateTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<PlayerRankingItem> playerRankingItemList;

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

    public List<PlayerRankingItem> getPlayerRankingItemList() {
        return playerRankingItemList;
    }

    public void setPlayerRankingItemList(List<PlayerRankingItem> playerRankingItemList) {
        this.playerRankingItemList = playerRankingItemList;
    }
}
