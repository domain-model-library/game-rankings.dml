package dml.gamerankings.service.repositoryset;

import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankRepository;
import dml.gamerankings.repository.PlayerRankResetTaskRepository;
import dml.gamerankings.repository.PlayerRankResetTaskSegmentRepository;

public interface RankingResetServiceRepositorySet {
    PlayerRankRepository getPlayerRankRepository();

    LeaderboardRepository getLeaderboardRepository();

    PlayerRankResetTaskRepository getPlayerRankResetTaskRepository();

    PlayerRankResetTaskSegmentRepository getPlayerRankResetTaskSegmentRepository();
}
