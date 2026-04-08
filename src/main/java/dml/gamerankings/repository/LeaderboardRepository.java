package dml.gamerankings.repository;

import dml.common.repository.CommonSingletonRepository;
import dml.gamerankings.entity.Leaderboard;

public interface LeaderboardRepository<E extends Leaderboard> extends CommonSingletonRepository<E> {
}
