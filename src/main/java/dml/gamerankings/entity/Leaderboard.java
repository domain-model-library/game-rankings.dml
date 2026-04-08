package dml.gamerankings.entity;

import java.util.List;

public interface Leaderboard {
    void setItemList(List<PlayerRankingItem> itemList);

    List<PlayerRankingItem> getItemList();
}
