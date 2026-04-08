package dml.gamerankings.service.repositoryset;

import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankingItemRepository;
import dml.gamerankings.repository.PlayerRankingItemUpdateTaskRepository;
import dml.gamerankings.repository.PlayerRankingItemUpdateTaskSegmentRepository;

public interface RankingServiceRepositorySet {
    PlayerRankingItemRepository getPlayerRankingItemRepository();

    LeaderboardRepository getLeaderboardRepository();

    PlayerRankingItemUpdateTaskRepository getPlayerRankingItemUpdateTaskRepository();

    PlayerRankingItemUpdateTaskSegmentRepository getPlayerRankingItemUpdateTaskSegmentRepository();
}
