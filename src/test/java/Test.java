import dml.common.repository.TestCommonRepository;
import dml.common.repository.TestCommonSingletonRepository;
import dml.gamerankings.entity.PlayerRankingChangeItem;
import dml.gamerankings.entity.RankItem;
import dml.gamerankings.entity.PlayerRank;
import dml.gamerankings.entity.PlayerRankingItem;
import dml.gamerankings.repository.*;
import dml.gamerankings.service.RankingChangeResetService;
import dml.gamerankings.service.RankingChangeService;
import dml.gamerankings.service.RankingResetService;
import dml.gamerankings.service.RankingService;
import dml.gamerankings.service.repositoryset.RankingChangeResetServiceRepositorySet;
import dml.gamerankings.service.repositoryset.RankingChangeServiceRepositorySet;
import dml.gamerankings.service.repositoryset.RankingResetServiceRepositorySet;
import dml.gamerankings.service.repositoryset.RankingServiceRepositorySet;
import dml.gamerankings.service.result.CalculateRankingResult;

import java.util.ArrayList;
import java.util.List;

public class Test {

    @org.junit.Test
    public void rankingAndRankChangeAndReset() {
        //准备10个玩家id
        long playerId1 = 1;
        long playerId2 = 2;
        long playerId3 = 3;
        long playerId4 = 4;
        long playerId5 = 5;
        long playerId6 = 6;
        long playerId7 = 7;
        long playerId8 = 8;
        long playerId9 = 9;
        long playerId10 = 10;

        //给10个玩家创建排行结果(PlayerRank)
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId1);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId2);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId3);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId4);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId5);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId6);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId7);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId8);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId9);
        RankingService.createPlayerRank(rankingServiceRepositorySet,
                new TestPlayerRank(), playerId10);

        //给10个玩家的排行结果设置排行指标值（模拟游戏里的原始排行指标值）
        ((TestPlayerRank) playerRankRepository.find(playerId1)).setMetricValue(10);
        ((TestPlayerRank) playerRankRepository.find(playerId2)).setMetricValue(20);
        ((TestPlayerRank) playerRankRepository.find(playerId3)).setMetricValue(30);
        ((TestPlayerRank) playerRankRepository.find(playerId4)).setMetricValue(40);
        ((TestPlayerRank) playerRankRepository.find(playerId5)).setMetricValue(50);
        ((TestPlayerRank) playerRankRepository.find(playerId6)).setMetricValue(60);
        ((TestPlayerRank) playerRankRepository.find(playerId7)).setMetricValue(70);
        ((TestPlayerRank) playerRankRepository.find(playerId8)).setMetricValue(80);
        ((TestPlayerRank) playerRankRepository.find(playerId9)).setMetricValue(90);
        ((TestPlayerRank) playerRankRepository.find(playerId10)).setMetricValue(100);

        //准备计算排行榜需要的参数：榜单取top几，从高到低还是从低到高排名
        int topN = 5;
        boolean rankingFromHighToLow = true;

        //计算排行榜
        long currentTime = 0;
        List<PlayerRankingItem> allRankingItems = getAllPlayerRankingItems(playerId1, playerId2, playerId3, playerId4, playerId5, playerId6, playerId7, playerId8, playerId9, playerId10);
        CalculateRankingResult result = RankingService.calculateRanking(rankingServiceRepositorySet,
                allRankingItems,
                new TestLeaderboard(),
                topN, rankingFromHighToLow,
                currentTime, 10);
        RankItem topItem = result.getLeaderboard().getItemList().get(0);
        assert topItem.getPlayerId().equals(playerId10);
        assert topItem.getRank() == 1;

        //给10个玩家创建排行变化项
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId1);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId2);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId3);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId4);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId5);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId6);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId7);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId8);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId9);
        RankingChangeService.createRankingChangeItem(rankingChangeServiceRepositorySet,
                playerId10);

        //记录玩家排名变化
        List<RankItem> allRankedItems = result.getAllRankedItems();
        RankingChangeService.recordRankingChange(rankingChangeServiceRepositorySet,
                allRankedItems, topN, currentTime, 10);

        //异步更新PlayerRank
        long segmentTimeoutMs = 60000;
        long maxTimeToReadyMs = 120000;
        String taskName = "PlayerRank-update-" + currentTime;
        String segmentId = RankingService.takePlayerRankUpdateSegmentToExecute(rankingServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        int count = RankingService.executePlayerRankUpdateSegment(rankingServiceRepositorySet,
                segmentId);
        assert count == 10;
        RankingService.completePlayerRankUpdateSegment(rankingServiceRepositorySet,
                segmentId);

        //验证第5名是否更新
        PlayerRank playerRank6 = RankingService.getPlayerRank(rankingServiceRepositorySet, playerId6);
        assert playerRank6.getRank() == 5;

        //异步更新PlayerRankingChangeItem
        taskName = "PlayerRankingChangeItem-update-" + currentTime;
        segmentId = RankingChangeService.takePlayerRankingChangeItemUpdateSegmentToExecute(rankingChangeServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        count = RankingChangeService.executePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);
        assert count == 5;
        RankingChangeService.completePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);

        //第二名的排行指标增加
        ((TestPlayerRank) playerRankRepository.find(playerId9)).setMetricValue(110);

        //重新计算排行榜
        currentTime += 1000;
        allRankingItems = getAllPlayerRankingItems(playerId1, playerId2, playerId3, playerId4, playerId5, playerId6, playerId7, playerId8, playerId9, playerId10);
        result = RankingService.calculateRanking(
                rankingServiceRepositorySet,
                allRankingItems,
                new TestLeaderboard(),
                topN, rankingFromHighToLow,
                currentTime, 10);
        topItem = result.getLeaderboard().getItemList().get(0);
        assert topItem.getPlayerId().equals(playerId9);
        assert topItem.getRank() == 1;

        //记录玩家排名变化
        allRankedItems = result.getAllRankedItems();
        RankingChangeService.recordRankingChange(rankingChangeServiceRepositorySet,
                allRankedItems, topN, currentTime, 10);

        //异步更新PlayerRank
        taskName = "PlayerRank-update-" + currentTime;
        segmentId = RankingService.takePlayerRankUpdateSegmentToExecute(rankingServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        count = RankingService.executePlayerRankUpdateSegment(rankingServiceRepositorySet,
                segmentId);
        assert count == 10;
        RankingService.completePlayerRankUpdateSegment(rankingServiceRepositorySet,
                segmentId);

        //异步更新PlayerRankingChangeItem
        taskName = "PlayerRankingChangeItem-update-" + currentTime;
        segmentId = RankingChangeService.takePlayerRankingChangeItemUpdateSegmentToExecute(rankingChangeServiceRepositorySet,
                taskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert segmentId != null;
        count = RankingChangeService.executePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);
        assert count == 5;
        RankingChangeService.completePlayerRankingChangeItemUpdateSegment(rankingChangeServiceRepositorySet,
                segmentId);

        //验证现在的第一名是从第二名变化过来的
        PlayerRankingChangeItem playerRankingChangeItem9 = RankingChangeService.getPlayerRankingChangeItem(rankingChangeServiceRepositorySet, playerId9);
        assert playerRankingChangeItem9.getPlayerId().equals(String.valueOf(playerId9));
        assert playerRankingChangeItem9.getCurrentRank() == 1;
        assert playerRankingChangeItem9.getLastRank() == 2;

        //重置排行榜
        currentTime += 1000;
        List<Object> allPlayerIdsForReset = new ArrayList<>();
        allPlayerIdsForReset.add(playerId1);
        allPlayerIdsForReset.add(playerId2);
        allPlayerIdsForReset.add(playerId3);
        allPlayerIdsForReset.add(playerId4);
        allPlayerIdsForReset.add(playerId5);
        allPlayerIdsForReset.add(playerId6);
        allPlayerIdsForReset.add(playerId7);
        allPlayerIdsForReset.add(playerId8);
        allPlayerIdsForReset.add(playerId9);
        allPlayerIdsForReset.add(playerId10);

        RankingResetService.resetRanking(rankingResetServiceRepositorySet,
                allPlayerIdsForReset, currentTime, 10);

        //验证Leaderboard已被删除
        assert RankingService.getLeaderboard(rankingServiceRepositorySet) == null;

        //异步执行PlayerRank重置任务段
        String resetTaskName = "PlayerRank-reset-" + currentTime;
        String resetSegmentId = RankingResetService.takePlayerRankResetSegmentToExecute(rankingResetServiceRepositorySet,
                resetTaskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert resetSegmentId != null;
        int resetCount = RankingResetService.executePlayerRankResetSegment(rankingResetServiceRepositorySet,
                resetSegmentId);
        assert resetCount == 10;
        RankingResetService.completePlayerRankResetSegment(rankingResetServiceRepositorySet,
                resetSegmentId);

        //验证PlayerRank的排名为0，指标值照旧不变
        PlayerRank resetItem9 = RankingService.getPlayerRank(rankingServiceRepositorySet, playerId9);
        assert resetItem9.getRank() == 0;
        PlayerRank resetItem10 = RankingService.getPlayerRank(rankingServiceRepositorySet, playerId10);
        assert resetItem10.getRank() == 0;

        //重置PlayerRankingChangeItem
        List<String> allPlayerIdStringsForReset = new ArrayList<>();
        allPlayerIdStringsForReset.add(String.valueOf(playerId1));
        allPlayerIdStringsForReset.add(String.valueOf(playerId2));
        allPlayerIdStringsForReset.add(String.valueOf(playerId3));
        allPlayerIdStringsForReset.add(String.valueOf(playerId4));
        allPlayerIdStringsForReset.add(String.valueOf(playerId5));
        allPlayerIdStringsForReset.add(String.valueOf(playerId6));
        allPlayerIdStringsForReset.add(String.valueOf(playerId7));
        allPlayerIdStringsForReset.add(String.valueOf(playerId8));
        allPlayerIdStringsForReset.add(String.valueOf(playerId9));
        allPlayerIdStringsForReset.add(String.valueOf(playerId10));

        RankingChangeResetService.resetRankingChange(rankingChangeResetServiceRepositorySet,
                allPlayerIdStringsForReset, currentTime, 10);

        //异步执行PlayerRankingChangeItem重置任务段
        String changeResetTaskName = "PlayerRankingChangeItem-reset-" + currentTime;
        String changeResetSegmentId = RankingChangeResetService.takePlayerRankingChangeItemResetSegmentToExecute(rankingChangeResetServiceRepositorySet,
                changeResetTaskName, currentTime, segmentTimeoutMs, maxTimeToReadyMs);
        assert changeResetSegmentId != null;
        int changeResetCount = RankingChangeResetService.executePlayerRankingChangeItemResetSegment(rankingChangeResetServiceRepositorySet,
                changeResetSegmentId);
        assert changeResetCount == 10;
        RankingChangeResetService.completePlayerRankingChangeItemResetSegment(rankingChangeResetServiceRepositorySet,
                changeResetSegmentId);

        //验证PlayerRankingChangeItem的上次排名和当前排名都已置零
        PlayerRankingChangeItem resetChangeItem9 = RankingChangeService.getPlayerRankingChangeItem(rankingChangeServiceRepositorySet, playerId9);
        assert resetChangeItem9.getLastRank() == 0;
        assert resetChangeItem9.getCurrentRank() == 0;
        PlayerRankingChangeItem resetChangeItem10 = RankingChangeService.getPlayerRankingChangeItem(rankingChangeServiceRepositorySet, playerId10);
        assert resetChangeItem10.getLastRank() == 0;
        assert resetChangeItem10.getCurrentRank() == 0;

    }

    private List<PlayerRankingItem> getAllPlayerRankingItems(long playerId1, long playerId2, long playerId3, long playerId4, long playerId5, long playerId6, long playerId7, long playerId8, long playerId9, long playerId10) {
        List<PlayerRankingItem> allRankingItems = new ArrayList<>();
        allRankingItems.add(new PlayerRankingItem(playerId1, ((TestPlayerRank) playerRankRepository.find(playerId1)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId2, ((TestPlayerRank) playerRankRepository.find(playerId2)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId3, ((TestPlayerRank) playerRankRepository.find(playerId3)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId4, ((TestPlayerRank) playerRankRepository.find(playerId4)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId5, ((TestPlayerRank) playerRankRepository.find(playerId5)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId6, ((TestPlayerRank) playerRankRepository.find(playerId6)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId7, ((TestPlayerRank) playerRankRepository.find(playerId7)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId8, ((TestPlayerRank) playerRankRepository.find(playerId8)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId9, ((TestPlayerRank) playerRankRepository.find(playerId9)).getMetricValue()));
        allRankingItems.add(new PlayerRankingItem(playerId10, ((TestPlayerRank) playerRankRepository.find(playerId10)).getMetricValue()));
        return allRankingItems;
    }

    PlayerRankRepository playerRankRepository = TestCommonRepository.instance(PlayerRankRepository.class);
    LeaderboardRepository leaderboardRepository = TestCommonSingletonRepository.instance(LeaderboardRepository.class);
    PlayerRankUpdateTaskRepository playerRankUpdateTaskRepository = TestCommonRepository.instance(PlayerRankUpdateTaskRepository.class);
    PlayerRankUpdateTaskSegmentRepository playerRankUpdateTaskSegmentRepository = TestCommonRepository.instance(PlayerRankUpdateTaskSegmentRepository.class);
    PlayerRankingChangeItemRepository playerRankingChangeItemRepository = TestCommonRepository.instance(PlayerRankingChangeItemRepository.class);
    PlayerRankingChangeItemUpdateTaskRepository playerRankingChangeItemUpdateTaskRepository = TestCommonRepository.instance(PlayerRankingChangeItemUpdateTaskRepository.class);
    PlayerRankingChangeItemUpdateTaskSegmentRepository playerRankingChangeItemUpdateTaskSegmentRepository = TestCommonRepository.instance(PlayerRankingChangeItemUpdateTaskSegmentRepository.class);
    PlayerRankResetTaskRepository playerRankResetTaskRepository = TestCommonRepository.instance(PlayerRankResetTaskRepository.class);
    PlayerRankResetTaskSegmentRepository playerRankResetTaskSegmentRepository = TestCommonRepository.instance(PlayerRankResetTaskSegmentRepository.class);
    PlayerRankingChangeItemResetTaskRepository playerRankingChangeItemResetTaskRepository = TestCommonRepository.instance(PlayerRankingChangeItemResetTaskRepository.class);
    PlayerRankingChangeItemResetTaskSegmentRepository playerRankingChangeItemResetTaskSegmentRepository = TestCommonRepository.instance(PlayerRankingChangeItemResetTaskSegmentRepository.class);

    RankingServiceRepositorySet rankingServiceRepositorySet = new RankingServiceRepositorySet() {
        @Override
        public PlayerRankRepository getPlayerRankRepository() {
            return playerRankRepository;
        }

        @Override
        public LeaderboardRepository getLeaderboardRepository() {
            return leaderboardRepository;
        }

        @Override
        public PlayerRankUpdateTaskRepository getPlayerRankUpdateTaskRepository() {
            return playerRankUpdateTaskRepository;
        }

        @Override
        public PlayerRankUpdateTaskSegmentRepository getPlayerRankUpdateTaskSegmentRepository() {
            return playerRankUpdateTaskSegmentRepository;
        }
    };

    RankingChangeServiceRepositorySet rankingChangeServiceRepositorySet = new RankingChangeServiceRepositorySet() {

        @Override
        public PlayerRankingChangeItemRepository getPlayerRankingChangeItemRepository() {
            return playerRankingChangeItemRepository;
        }

        @Override
        public PlayerRankingChangeItemUpdateTaskRepository getPlayerRankingChangeItemUpdateTaskRepository() {
            return playerRankingChangeItemUpdateTaskRepository;
        }

        @Override
        public PlayerRankingChangeItemUpdateTaskSegmentRepository getPlayerRankingChangeItemUpdateTaskSegmentRepository() {
            return playerRankingChangeItemUpdateTaskSegmentRepository;
        }
    };

    RankingResetServiceRepositorySet rankingResetServiceRepositorySet = new RankingResetServiceRepositorySet() {

        @Override
        public PlayerRankRepository getPlayerRankRepository() {
            return playerRankRepository;
        }

        @Override
        public LeaderboardRepository getLeaderboardRepository() {
            return leaderboardRepository;
        }

        @Override
        public PlayerRankResetTaskRepository getPlayerRankResetTaskRepository() {
            return playerRankResetTaskRepository;
        }

        @Override
        public PlayerRankResetTaskSegmentRepository getPlayerRankResetTaskSegmentRepository() {
            return playerRankResetTaskSegmentRepository;
        }
    };

    RankingChangeResetServiceRepositorySet rankingChangeResetServiceRepositorySet = new RankingChangeResetServiceRepositorySet() {

        @Override
        public PlayerRankingChangeItemRepository getPlayerRankingChangeItemRepository() {
            return playerRankingChangeItemRepository;
        }

        @Override
        public PlayerRankingChangeItemResetTaskRepository getPlayerRankingChangeItemResetTaskRepository() {
            return playerRankingChangeItemResetTaskRepository;
        }

        @Override
        public PlayerRankingChangeItemResetTaskSegmentRepository getPlayerRankingChangeItemResetTaskSegmentRepository() {
            return playerRankingChangeItemResetTaskSegmentRepository;
        }
    };

}
