package dml.gamerankings.service;

import dml.gamerankings.entity.PlayerRank;
import dml.gamerankings.entity.PlayerRankResetTask;
import dml.gamerankings.entity.PlayerRankResetTaskSegment;
import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankRepository;
import dml.gamerankings.repository.PlayerRankResetTaskRepository;
import dml.gamerankings.repository.PlayerRankResetTaskSegmentRepository;
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

        submitPlayerRankResetTask(repositorySet, allPlayerIds, currentTime, itemsUpdateBatchSize);
    }

    private static void submitPlayerRankResetTask(RankingResetServiceRepositorySet repositorySet,
                                                   List<Object> allPlayerIds, long currentTime,
                                                   int itemsUpdateBatchSize) {
        if (allPlayerIds == null || allPlayerIds.isEmpty()) {
            return;
        }
        PlayerRankResetTaskRepository playerRankResetTaskRepository = repositorySet.getPlayerRankResetTaskRepository();
        PlayerRankResetTaskSegmentRepository playerRankResetTaskSegmentRepository = repositorySet.getPlayerRankResetTaskSegmentRepository();

        String taskName = "PlayerRank-reset-" + currentTime;
        PlayerRankResetTask task = new PlayerRankResetTask();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankResetTaskRepository, playerRankResetTaskSegmentRepository);
        LargeScaleTaskService.createTask(largeScaleTaskServiceRepositorySet, taskName, task, currentTime);

        for (int i = 0; i < allPlayerIds.size(); i += itemsUpdateBatchSize) {
            int endIdx = Math.min(i + itemsUpdateBatchSize, allPlayerIds.size());
            List<Object> batch = allPlayerIds.subList(i, endIdx);
            String segmentId = taskName + "-" + i;
            PlayerRankResetTaskSegment segment = new PlayerRankResetTaskSegment(segmentId);
            segment.setPlayerIdList(new ArrayList<>(batch));
            LargeScaleTaskService.addTaskSegment(largeScaleTaskServiceRepositorySet, taskName, segment);
        }
        LargeScaleTaskService.setTaskReadyToProcess(largeScaleTaskServiceRepositorySet, taskName);
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(
            PlayerRankResetTaskRepository playerRankResetTaskRepository,
            PlayerRankResetTaskSegmentRepository playerRankResetTaskSegmentRepository) {
        return new LargeScaleTaskServiceRepositorySet() {

            @Override
            public LargeScaleTaskRepository getLargeScaleTaskRepository() {
                return playerRankResetTaskRepository;
            }

            @Override
            public LargeScaleTaskSegmentRepository getLargeScaleTaskSegmentRepository() {
                return playerRankResetTaskSegmentRepository;
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

    public static String takePlayerRankResetSegmentToExecute(RankingResetServiceRepositorySet repositorySet,
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
        PlayerRankResetTaskRepository playerRankResetTaskRepository = repositorySet.getPlayerRankResetTaskRepository();
        PlayerRankResetTaskSegmentRepository playerRankResetTaskSegmentRepository = repositorySet.getPlayerRankResetTaskSegmentRepository();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankResetTaskRepository, playerRankResetTaskSegmentRepository);
        return largeScaleTaskServiceRepositorySet;
    }

    public static int executePlayerRankResetSegment(RankingResetServiceRepositorySet repositorySet,
                                                    String segmentId) {
        PlayerRankResetTaskSegmentRepository playerRankResetTaskSegmentRepository = repositorySet.getPlayerRankResetTaskSegmentRepository();
        PlayerRankRepository<PlayerRank, Object> playerRankRepository = repositorySet.getPlayerRankRepository();

        PlayerRankResetTaskSegment segment = playerRankResetTaskSegmentRepository.find(segmentId);
        if (segment == null) {
            return 0;
        }
        List<Object> playerIdList = segment.getPlayerIdList();
        if (playerIdList == null || playerIdList.isEmpty()) {
            return 0;
        }
        for (Object playerId : playerIdList) {
            PlayerRank existing = playerRankRepository.take(playerId);
            if (existing != null) {
                existing.setRank(0);
            }
        }
        return playerIdList.size();
    }

    public static void completePlayerRankResetSegment(RankingResetServiceRepositorySet repositorySet,
                                                      String segmentId) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        LargeScaleTaskService.completeTaskSegment(largeScaleTaskServiceRepositorySet, segmentId);
    }
}
