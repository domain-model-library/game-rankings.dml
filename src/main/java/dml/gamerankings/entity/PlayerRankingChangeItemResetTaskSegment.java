package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;

public class PlayerRankingChangeItemResetTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<String> playerIdList;

    public PlayerRankingChangeItemResetTaskSegment() {
    }

    public PlayerRankingChangeItemResetTaskSegment(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<String> getPlayerIdList() {
        return playerIdList;
    }

    public void setPlayerIdList(List<String> playerIdList) {
        this.playerIdList = playerIdList;
    }
}
