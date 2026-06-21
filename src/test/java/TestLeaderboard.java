import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.RankItem;

import java.util.List;

public class TestLeaderboard implements Leaderboard {
    private List<RankItem> itemList;

    @Override
    public void setItemList(List<RankItem> itemList) {
        this.itemList = itemList;
    }

    @Override
    public List<RankItem> getItemList() {
        return itemList;
    }
}
