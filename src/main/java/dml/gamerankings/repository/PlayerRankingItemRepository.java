package dml.gamerankings.repository;

import dml.common.repository.CommonRepository;
import dml.gamerankings.entity.PlayerRankingItem;

public interface PlayerRankingItemRepository<E extends PlayerRankingItem, PID> extends CommonRepository<E, PID> {
}
