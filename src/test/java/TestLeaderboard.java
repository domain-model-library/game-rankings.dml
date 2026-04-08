import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.PlayerRankingItem;

import java.util.List;

public class TestLeaderboard implements Leaderboard {
    private List<PlayerRankingItem> itemList;

    @Override
    public void setItemList(List<PlayerRankingItem> itemList) {
        this.itemList = itemList;
    }

    @Override
    public List<PlayerRankingItem> getItemList() {
        return itemList;
    }
}
