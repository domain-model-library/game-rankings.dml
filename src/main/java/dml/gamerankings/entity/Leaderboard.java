package dml.gamerankings.entity;

import java.util.List;

public interface Leaderboard {
    void setItemList(List<RankItem> itemList);

    List<RankItem> getItemList();
}
