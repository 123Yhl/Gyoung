package cn.yhl.kvstore2pcsystem.moudle_coordinator.core;

import org.springframework.scheduling.annotation.Scheduled;

public class TimingDetectionNode {
    //每隔30秒刷新一次节点
    @Scheduled(cron = "0/30 * * * * ?")
    private void checkNodes() {
        NodeManager.checkAlive();
    }
}
