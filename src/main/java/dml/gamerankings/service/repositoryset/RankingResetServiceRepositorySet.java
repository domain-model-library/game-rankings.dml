package dml.gamerankings.service.repositoryset;

import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankingItemRepository;
import dml.gamerankings.repository.PlayerRankingItemResetTaskRepository;
import dml.gamerankings.repository.PlayerRankingItemResetTaskSegmentRepository;

public interface RankingResetServiceRepositorySet {
    PlayerRankingItemRepository getPlayerRankingItemRepository();

    LeaderboardRepository getLeaderboardRepository();

    PlayerRankingItemResetTaskRepository getPlayerRankingItemResetTaskRepository();

    PlayerRankingItemResetTaskSegmentRepository getPlayerRankingItemResetTaskSegmentRepository();
}
