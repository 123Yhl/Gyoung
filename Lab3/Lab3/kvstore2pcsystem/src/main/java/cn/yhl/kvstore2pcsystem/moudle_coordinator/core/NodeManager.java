package cn.yhl.kvstore2pcsystem.moudle_coordinator.core;


import cn.yhl.kvstore2pcsystem.moudle_coordinator.coordinatorCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Conditional({coordinatorCondition.class})
@Component
public class NodeManager {
    public static Integer modcount = 0;
    public static ConcurrentHashMap<Participant, Date> participants = new ConcurrentHashMap<>();

    //刷新数据节点的活跃时间
    public static boolean refreshAliveNode(String ip, String port) {
        for (Map.Entry<Participant, Date> entry : participants.entrySet()) {
            Participant key = entry.getKey();
            if (key.getIp().equals(ip) && key.getPort().equals(port)) {
                entry.setValue(new Date());
                modcount++;
                return true;
            }
        }
        //没有找到需要刷新的节点,刷新失败
        return false;
    }

    //获取当前存活的参与者列表
    public static ArrayList<Participant> getAliveParticipantList() {
        ArrayList<Participant> aliveParticipants = new ArrayList<Participant>();
        // System.out.println("in getAliveP  participants entrySet数目为"+participants.entrySet().size());
        for (Map.Entry<Participant, Date> entry : participants.entrySet()) {
            //数据节点存活
            if (entry != null && new Date().getTime() - entry.getValue().getTime() < Const.ALIVE_CHECK_INTERVAL) {
                aliveParticipants.add(entry.getKey());
            } else if (entry == null) {
                continue;
            }
            //数据节点心跳不及时  认为已经掉线 删除
            else {
                participants.remove(entry.getKey());
            }
        }
        return aliveParticipants;
    }

    //定时任务移除死节点
    public static void checkAlive() {
        System.out.println("[DEBUG]检测节点！");
        for (Map.Entry<Participant, Date> entry : participants.entrySet()) {
            //数据节点心跳不及时  认为已经掉线 删除
            if (new Date().getTime() - entry.getValue().getTime() >= Const.ALIVE_CHECK_INTERVAL) {
                participants.remove(entry.getKey());
            }
        }
    }
}
