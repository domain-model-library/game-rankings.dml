import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.PlayerRank;

import java.util.List;

public class TestLeaderboard implements Leaderboard {
    private List<PlayerRank> itemList;

    @Override
    public void setItemList(List<PlayerRank> itemList) {
        this.itemList = itemList;
    }

    @Override
    public List<PlayerRank> getItemList() {
        return itemList;
    }
}
