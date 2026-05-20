package dml.gamerankings.entity;

import dml.largescaletaskmanagement.entity.LargeScaleTaskSegmentBase;

import java.util.List;

public class PlayerRankResetTaskSegment extends LargeScaleTaskSegmentBase {
    private String id;
    private List<Object> playerIdList;

    public PlayerRankResetTaskSegment() {
    }

    public PlayerRankResetTaskSegment(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public List<Object> getPlayerIdList() {
        return playerIdList;
    }

    public void setPlayerIdList(List<Object> playerIdList) {
        this.playerIdList = playerIdList;
    }
}
