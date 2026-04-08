package dml.gamerankings.service;

import dml.gamerankings.entity.PlayerRankingItem;
import dml.gamerankings.entity.PlayerRankingItemResetTask;
import dml.gamerankings.entity.PlayerRankingItemResetTaskSegment;
import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankingItemRepository;
import dml.gamerankings.repository.PlayerRankingItemResetTaskRepository;
import dml.gamerankings.repository.PlayerRankingItemResetTaskSegmentRepository;
import dml.gamerankings.service.repositoryset.RankingResetServiceRepositorySet;
import dml.largescaletaskmanagement.entity.ResetSegmentToProcessIfTimeout;
import dml.largescaletaskmanagement.repository.LargeScaleTaskRepository;
import dml.largescaletaskmanagement.repository.LargeScaleTaskSegmentRepository;
import dml.largescaletaskmanagement.repository.SegmentProcessingTimeoutHandlingStrategyRepository;
import dml.largescaletaskmanagement.service.LargeScaleTaskService;
import dml.largescaletaskmanagement.service.repositoryset.LargeScaleTaskServiceRepositorySet;
import dml.largescaletaskmanagement.service.result.TakeTaskSegmentToExecuteResult;

import java.util.ArrayList;
import java.util.List;

public class RankingResetService {

    public static void resetRanking(RankingResetServiceRepositorySet repositorySet,
                                    List<Object> allPlayerIds,
                                    long currentTime, int itemsUpdateBatchSize) {
        LeaderboardRepository leaderboardRepository = repositorySet.getLeaderboardRepository();
        leaderboardRepository.remove();

        submitRankingItemResetTask(repositorySet, allPlayerIds, currentTime, itemsUpdateBatchSize);
    }

    private static void submitRankingItemResetTask(RankingResetServiceRepositorySet repositorySet,
                                                   List<Object> allPlayerIds, long currentTime,
                                                   int itemsUpdateBatchSize) {
        if (allPlayerIds == null || allPlayerIds.isEmpty()) {
            return;
        }
        PlayerRankingItemResetTaskRepository playerRankingItemResetTaskRepository = repositorySet.getPlayerRankingItemResetTaskRepository();
        PlayerRankingItemResetTaskSegmentRepository playerRankingItemResetTaskSegmentRepository = repositorySet.getPlayerRankingItemResetTaskSegmentRepository();

        String taskName = "PlayerRankingItem-reset-" + currentTime;
        PlayerRankingItemResetTask task = new PlayerRankingItemResetTask();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingItemResetTaskRepository, playerRankingItemResetTaskSegmentRepository);
        LargeScaleTaskService.createTask(largeScaleTaskServiceRepositorySet, taskName, task, currentTime);

        for (int i = 0; i < allPlayerIds.size(); i += itemsUpdateBatchSize) {
            int endIdx = Math.min(i + itemsUpdateBatchSize, allPlayerIds.size());
            List<Object> batch = allPlayerIds.subList(i, endIdx);
            String segmentId = taskName + "-" + i;
            PlayerRankingItemResetTaskSegment segment = new PlayerRankingItemResetTaskSegment(segmentId);
            segment.setPlayerIdList(new ArrayList<>(batch));
            LargeScaleTaskService.addTaskSegment(largeScaleTaskServiceRepositorySet, taskName, segment);
        }
        LargeScaleTaskService.setTaskReadyToProcess(largeScaleTaskServiceRepositorySet, taskName);
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(
            PlayerRankingItemResetTaskRepository playerRankingItemResetTaskRepository,
            PlayerRankingItemResetTaskSegmentRepository playerRankingItemResetTaskSegmentRepository) {
        return new LargeScaleTaskServiceRepositorySet() {

            @Override
            public LargeScaleTaskRepository getLargeScaleTaskRepository() {
                return playerRankingItemResetTaskRepository;
            }

            @Override
            public LargeScaleTaskSegmentRepository getLargeScaleTaskSegmentRepository() {
                return playerRankingItemResetTaskSegmentRepository;
            }

            @Override
            public SegmentProcessingTimeoutHandlingStrategyRepository getSegmentProcessingTimeoutHandlingStrategyRepository() {
                return new SegmentProcessingTimeoutHandlingStrategyRepository() {

                    @Override
                    public Object get() {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public Object take() {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public void put(Object o) {
                    }

                    @Override
                    public Object putIfAbsent(Object o) {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public Object takeOrPutIfAbsent(Object o) {
                        return new ResetSegmentToProcessIfTimeout();
                    }

                    @Override
                    public Object remove() {
                        return new ResetSegmentToProcessIfTimeout();
                    }
                };
            }
        };
    }

    public static String takePlayerRankingItemResetSegmentToExecute(RankingResetServiceRepositorySet repositorySet,
                                                                     String taskName, long currentTime, long segmentTimeoutMs, long maxTimeToReadyMs) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        TakeTaskSegmentToExecuteResult result = LargeScaleTaskService.takeTaskSegmentToExecute(
                largeScaleTaskServiceRepositorySet, taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        if (result.isTaskCompleted()) {
            LargeScaleTaskService.removeTask(largeScaleTaskServiceRepositorySet, taskName);
        }
        if (result.getTaskSegment() == null) {
            return null;
        }
        return String.valueOf(result.getTaskSegment().getId());
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(RankingResetServiceRepositorySet repositorySet) {
        PlayerRankingItemResetTaskRepository playerRankingItemResetTaskRepository = repositorySet.getPlayerRankingItemResetTaskRepository();
        PlayerRankingItemResetTaskSegmentRepository playerRankingItemResetTaskSegmentRepository = repositorySet.getPlayerRankingItemResetTaskSegmentRepository();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingItemResetTaskRepository, playerRankingItemResetTaskSegmentRepository);
        return largeScaleTaskServiceRepositorySet;
    }

    public static int executePlayerRankingItemResetSegment(RankingResetServiceRepositorySet repositorySet,
                                                           String segmentId) {
        PlayerRankingItemResetTaskSegmentRepository playerRankingItemResetTaskSegmentRepository = repositorySet.getPlayerRankingItemResetTaskSegmentRepository();
        PlayerRankingItemRepository<PlayerRankingItem, Object> playerRankingItemRepository = repositorySet.getPlayerRankingItemRepository();

        PlayerRankingItemResetTaskSegment segment = playerRankingItemResetTaskSegmentRepository.find(segmentId);
        if (segment == null) {
            return 0;
        }
        List<Object> playerIdList = segment.getPlayerIdList();
        if (playerIdList == null || playerIdList.isEmpty()) {
            return 0;
        }
        for (Object playerId : playerIdList) {
            PlayerRankingItem existing = playerRankingItemRepository.take(playerId);
            if (existing != null) {
                existing.setMetricValue(0);
                existing.setRank(0);
            }
        }
        return playerIdList.size();
    }

    public static void completePlayerRankingItemResetSegment(RankingResetServiceRepositorySet repositorySet,
                                                              String segmentId) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        LargeScaleTaskService.completeTaskSegment(largeScaleTaskServiceRepositorySet, segmentId);
    }
}
