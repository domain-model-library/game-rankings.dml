package dml.gamerankings.service.result;

import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.RankItem;

import java.util.List;

public class CalculateRankingResult {
    private final Leaderboard leaderboard;
    private final List<RankItem> allRankedItems;

    public CalculateRankingResult(Leaderboard leaderboard, List<RankItem> allRankedItems) {
        this.leaderboard = leaderboard;
        this.allRankedItems = allRankedItems;
    }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public List<RankItem> getAllRankedItems() {
        return allRankedItems;
    }
}
