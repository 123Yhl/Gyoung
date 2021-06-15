package cn.yhl.kvstore2pcsystem.moudle_participant.core;

import cn.yhl.kvstore2pcsystem.moudle_participant.participantCondition;
import cn.yhl.kvstore2pcsystem.moudle_participant.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;

@Conditional({participantCondition.class})
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling
public class HeartBeatToCoo {
    @Autowired
    Participant participant;

    @Scheduled(cron = "0/5 * * * * ?")
    private void heartBeat(){
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("ip", participant.getIp());
        stringStringHashMap.put("port", participant.getPort());
        String s = HttpClientUtil.doPost("http://" + participant.getCo_addr() + ":" + "8080" + "/coordinate/heartbeat", stringStringHashMap);
        System.out.println("心跳结果："+s);
    }
}
