package dml.gamerankings.entity;

import java.util.List;

public interface Leaderboard {
    void setItemList(List<PlayerRank> itemList);

    List<PlayerRank> getItemList();
}
