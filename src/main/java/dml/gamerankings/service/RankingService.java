package dml.gamerankings.service;

import dml.gamerankings.entity.Leaderboard;
import dml.gamerankings.entity.PlayerRankingItem;
import dml.gamerankings.entity.PlayerRankingItemUpdateTask;
import dml.gamerankings.entity.PlayerRankingItemUpdateTaskSegment;
import dml.gamerankings.repository.LeaderboardRepository;
import dml.gamerankings.repository.PlayerRankingItemRepository;
import dml.gamerankings.repository.PlayerRankingItemUpdateTaskRepository;
import dml.gamerankings.repository.PlayerRankingItemUpdateTaskSegmentRepository;
import dml.gamerankings.service.repositoryset.RankingServiceRepositorySet;
import dml.gamerankings.service.result.CalculateRankingResult;
import dml.largescaletaskmanagement.entity.ResetSegmentToProcessIfTimeout;
import dml.largescaletaskmanagement.repository.LargeScaleTaskRepository;
import dml.largescaletaskmanagement.repository.LargeScaleTaskSegmentRepository;
import dml.largescaletaskmanagement.repository.SegmentProcessingTimeoutHandlingStrategyRepository;
import dml.largescaletaskmanagement.service.LargeScaleTaskService;
import dml.largescaletaskmanagement.service.repositoryset.LargeScaleTaskServiceRepositorySet;
import dml.largescaletaskmanagement.service.result.TakeTaskSegmentToExecuteResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankingService {
    public static PlayerRankingItem createRankingItem(RankingServiceRepositorySet repositorySet,
                                                      PlayerRankingItem playerRankingItem, Object playerId) {
        PlayerRankingItemRepository<PlayerRankingItem, Object> playerRankingItemRepository = repositorySet.getPlayerRankingItemRepository();

        playerRankingItem.setPlayerId(playerId);
        PlayerRankingItem exists = playerRankingItemRepository.putIfAbsent(playerRankingItem);
        if (exists != null) {
            return exists;
        }
        return playerRankingItem;
    }

    public static void setRankingMetricValue(RankingServiceRepositorySet repositorySet,
                                             Object playerId, long metricValue) {
        PlayerRankingItemRepository<PlayerRankingItem, Object> playerRankingItemRepository = repositorySet.getPlayerRankingItemRepository();

        PlayerRankingItem playerRankingItem = playerRankingItemRepository.take(playerId);
        playerRankingItem.setMetricValue(metricValue);
    }

    public static CalculateRankingResult calculateRanking(RankingServiceRepositorySet repositorySet,
                                                          List<PlayerRankingItem> allRankingItems,
                                                          Leaderboard newLeaderboard,
                                                          int topN, boolean rankingFromHighToLow,
                                                          long currentTime, int itemsUpdateBatchSize) {
        PlayerRankingItem[] items = allRankingItems.toArray(new PlayerRankingItem[0]);
        int n = items.length;

        // Compute how many bits are needed for array indices (low bits) and metric values (high bits).
        // maxMetricValue determines metricBits; n determines indexBits.
        // Encoding: (metricValue << indexBits) | arrayIndex — sorts correctly as a signed long
        // as long as metricBits + indexBits <= 63 (sign bit unused).
        int indexBits = (n <= 1) ? 0 : (Long.SIZE - Long.numberOfLeadingZeros(n - 1));

        long[] encoded = new long[n];
        for (int i = 0; i < n; i++) {
            encoded[i] = (items[i].getMetricValue() << indexBits) | i;
        }

        Arrays.sort(encoded);

        // Walk the sorted array assigning ranks 1..n.
        // rankingFromHighToLow=true  → highest encoded value (= highest metric) is rank 1, iterate backwards.
        // rankingFromHighToLow=false → lowest encoded value (= lowest metric) is rank 1, iterate forwards.
        long indexMask = (1L << indexBits) - 1;
        List<PlayerRankingItem> allRankedItems = new ArrayList<>(n);
        List<PlayerRankingItem> topItems = new ArrayList<>(Math.min(topN, n));

        for (int rank = 1; rank <= n; rank++) {
            int sortedPos = rankingFromHighToLow ? (n - rank) : (rank - 1);
            int itemIndex = (int) (encoded[sortedPos] & indexMask);
            PlayerRankingItem item = items[itemIndex];
            item.setRank(rank);
            allRankedItems.add(item);
            if (rank <= topN) {
                topItems.add(item);
            }
        }

        // Take existing leaderboard or create it for the first time.
        LeaderboardRepository<Leaderboard> leaderboardRepository = repositorySet.getLeaderboardRepository();
        Leaderboard leaderboard = leaderboardRepository.takeOrPutIfAbsent(newLeaderboard);
        leaderboard.setItemList(topItems);

        PlayerRankingItemUpdateTaskRepository playerRankingItemUpdateTaskRepository = repositorySet.getPlayerRankingItemUpdateTaskRepository();
        PlayerRankingItemUpdateTaskSegmentRepository playerRankingItemUpdateTaskSegmentRepository = repositorySet.getPlayerRankingItemUpdateTaskSegmentRepository();
        submitRankingItemUpdateTask(playerRankingItemUpdateTaskRepository, playerRankingItemUpdateTaskSegmentRepository,
                allRankedItems, currentTime, itemsUpdateBatchSize);

        return new CalculateRankingResult(leaderboard, allRankedItems);
    }

    private static void submitRankingItemUpdateTask(PlayerRankingItemUpdateTaskRepository playerRankingItemUpdateTaskRepository,
                                                    PlayerRankingItemUpdateTaskSegmentRepository playerRankingItemUpdateTaskSegmentRepository,
                                                    List<PlayerRankingItem> allRankedItems, long currentTime,
                                                    int itemsUpdateBatchSize) {
        if (allRankedItems == null || allRankedItems.isEmpty()) {
            return;
        }
        String taskName = "PlayerRankingItem-update-" + currentTime;
        PlayerRankingItemUpdateTask task = new PlayerRankingItemUpdateTask();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingItemUpdateTaskRepository, playerRankingItemUpdateTaskSegmentRepository);
        LargeScaleTaskService.createTask(largeScaleTaskServiceRepositorySet, taskName, task, currentTime);

        for (int i = 0; i < allRankedItems.size(); i += itemsUpdateBatchSize) {
            int endIdx = Math.min(i + itemsUpdateBatchSize, allRankedItems.size());
            List<PlayerRankingItem> batch = allRankedItems.subList(i, endIdx);
            String segmentId = taskName + "-" + i;
            PlayerRankingItemUpdateTaskSegment segment = new PlayerRankingItemUpdateTaskSegment(segmentId);
            segment.setPlayerRankingItemList(new ArrayList<>(batch));
            LargeScaleTaskService.addTaskSegment(largeScaleTaskServiceRepositorySet, taskName, segment);
        }
        LargeScaleTaskService.setTaskReadyToProcess(largeScaleTaskServiceRepositorySet, taskName);
    }

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(
            PlayerRankingItemUpdateTaskRepository playerRankingItemUpdateTaskRepository,
            PlayerRankingItemUpdateTaskSegmentRepository playerRankingItemUpdateTaskSegmentRepository) {
        return new LargeScaleTaskServiceRepositorySet() {

            @Override
            public LargeScaleTaskRepository getLargeScaleTaskRepository() {
                return playerRankingItemUpdateTaskRepository;
            }

            @Override
            public LargeScaleTaskSegmentRepository getLargeScaleTaskSegmentRepository() {
                return playerRankingItemUpdateTaskSegmentRepository;
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

    public static PlayerRankingItem getPlayerRankingItem(RankingServiceRepositorySet repositorySet,
                                                         long playerId) {
        PlayerRankingItemRepository<PlayerRankingItem, Object> playerRankingItemRepository = repositorySet.getPlayerRankingItemRepository();
        return playerRankingItemRepository.find(playerId);
    }

    public static String takePlayerRankingItemUpdateSegmentToExecute(RankingServiceRepositorySet repositorySet,
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

    private static LargeScaleTaskServiceRepositorySet getLargeScaleTaskServiceRepositorySet(RankingServiceRepositorySet repositorySet) {
        PlayerRankingItemUpdateTaskRepository playerRankingItemUpdateTaskRepository = repositorySet.getPlayerRankingItemUpdateTaskRepository();
        PlayerRankingItemUpdateTaskSegmentRepository playerRankingItemUpdateTaskSegmentRepository = repositorySet.getPlayerRankingItemUpdateTaskSegmentRepository();
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet =
                getLargeScaleTaskServiceRepositorySet(playerRankingItemUpdateTaskRepository, playerRankingItemUpdateTaskSegmentRepository);
        return largeScaleTaskServiceRepositorySet;
    }

    public static int executePlayerRankingItemUpdateSegment(RankingServiceRepositorySet repositorySet,
                                                            String segmentId) {
        PlayerRankingItemUpdateTaskSegmentRepository playerRankingItemUpdateTaskSegmentRepository = repositorySet.getPlayerRankingItemUpdateTaskSegmentRepository();
        PlayerRankingItemRepository<PlayerRankingItem, Object> playerRankingItemRepository = repositorySet.getPlayerRankingItemRepository();

        PlayerRankingItemUpdateTaskSegment segment = playerRankingItemUpdateTaskSegmentRepository.find(segmentId);
        if (segment == null) {
            return 0;
        }
        List<PlayerRankingItem> playerRankingItemList = segment.getPlayerRankingItemList();
        if (playerRankingItemList == null || playerRankingItemList.isEmpty()) {
            return 0;
        }
        for (PlayerRankingItem item : playerRankingItemList) {
            PlayerRankingItem existing = playerRankingItemRepository.take(item.getPlayerId());
            if (existing == null) {
                playerRankingItemRepository.put(item);
            } else {
                existing.setRank(item.getRank());
            }
        }
        return playerRankingItemList.size();
    }

    public static void completePlayerRankingItemUpdateSegment(RankingServiceRepositorySet repositorySet,
                                                              String segmentId) {
        LargeScaleTaskServiceRepositorySet largeScaleTaskServiceRepositorySet = getLargeScaleTaskServiceRepositorySet(repositorySet);

        LargeScaleTaskService.completeTaskSegment(largeScaleTaskServiceRepositorySet, segmentId);
    }


    public static Leaderboard getLeaderboard(RankingServiceRepositorySet repositorySet) {
        LeaderboardRepository<Leaderboard> leaderboardRepository = repositorySet.getLeaderboardRepository();
        return leaderboardRepository.get();
    }
}
