package dml.gamerankings.service.result;

import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.PlayerRankingItem;

import java.util.List;

public class CalculateRankingResult {
    private final Leaderboard leaderboard;
    private final List<PlayerRankingItem> allRankedItems;

    public CalculateRankingResult(Leaderboard leaderboard, List<PlayerRankingItem> allRankedItems) {
        this.leaderboard = leaderboard;
        this.allRankedItems = allRankedItems;
    }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public List<PlayerRankingItem> getAllRankedItems() {
        return allRankedItems;
    }
}
