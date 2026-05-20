package dml.gamerankings.service.repositoryset;

import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankRepository;
import dml.gamerankings.repository.PlayerRankUpdateTaskRepository;
import dml.gamerankings.repository.PlayerRankUpdateTaskSegmentRepository;

public interface RankingServiceRepositorySet {
    PlayerRankRepository getPlayerRankRepository();

    LeaderboardRepository getLeaderboardRepository();

    PlayerRankUpdateTaskRepository getPlayerRankUpdateTaskRepository();

    PlayerRankUpdateTaskSegmentRepository getPlayerRankUpdateTaskSegmentRepository();
}
