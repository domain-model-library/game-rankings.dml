package dml.gamerankings.repository;

import dml.common.repository.CommonRepository;
import dml.gamerankings.entity.PlayerRank;

public interface PlayerRankRepository<E extends PlayerRank, PID> extends CommonRepository<E, PID> {
}
