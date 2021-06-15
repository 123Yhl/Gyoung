package cn.yhl.kvstore2pcsystem.moudle_coordinator.controller;


import cn.yhl.kvstore2pcsystem.moudle_coordinator.coordinatorCondition;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.NodeManager;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.Participant;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.util.HttpClientUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;

@Conditional({coordinatorCondition.class})
@RestController
@RequestMapping("/coordinate")
public class CoordinateController {
    @PostMapping("/heartbeat")
    public String heartbeat(HttpServletRequest request) {
        String ip = request.getParameter("ip");
        String port = request.getParameter("port");
        boolean flag = NodeManager.refreshAliveNode(ip, port);
        return flag ? "true":"false";
    }

    @PostMapping("/register")
    public String register(HttpServletRequest request) {
        String ip = request.getParameter("ip");
        String port = request.getParameter("port");
        Participant participant = new Participant();
        participant.setIp(ip);
        participant.setPort(port);
        NodeManager.participants.put(participant,new Date());
        System.out.println("参与者："+ip+port+"注册到协调者！");
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        Participant res = null;
        for (Participant participant1 : aliveParticipantList) {
            if(participant1.getIp()!=ip){
                res = participant1;
            }
        }
        String s = HttpClientUtil.doPostJson("http://"+res.getIp()+":"+res.getPort()+"/participant/copy",
                JSONObject.toJSONString(request));
        return JSONObject.toJSONString(s);
    }
}
