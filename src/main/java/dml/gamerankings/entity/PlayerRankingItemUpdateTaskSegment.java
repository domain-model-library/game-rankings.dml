package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;

/**
 * PlayerRankingItem 批量更新任务段实体。
 * 包含一批需要更新的 PlayerRankingItem 记录。
 */
public class PlayerRankingItemUpdateTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<PlayerRankingItem> playerRankingItemList;

    public PlayerRankingItemUpdateTaskSegment() {
    }

    public PlayerRankingItemUpdateTaskSegment(String id) {
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
