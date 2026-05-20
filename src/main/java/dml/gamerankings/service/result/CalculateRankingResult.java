package dml.gamerankings.service.result;

import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.PlayerRank;

import java.util.List;

public class CalculateRankingResult {
    private final Leaderboard leaderboard;
    private final List<PlayerRank> allRankedItems;

    public CalculateRankingResult(Leaderboard leaderboard, List<PlayerRank> allRankedItems) {
        this.leaderboard = leaderboard;
        this.allRankedItems = allRankedItems;
    }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public List<PlayerRank> getAllRankedItems() {
        return allRankedItems;
    }
}
