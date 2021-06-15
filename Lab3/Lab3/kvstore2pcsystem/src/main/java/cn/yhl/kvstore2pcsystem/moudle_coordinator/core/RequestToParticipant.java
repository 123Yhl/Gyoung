package cn.yhl.kvstore2pcsystem.moudle_coordinator.core;


import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespRequest;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespResponse;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.util.HttpClientUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.Callable;

public class RequestToParticipant implements Callable<RespResponse> {
    private RespRequest request;
    private Participant participant;
    private String command;

    public RequestToParticipant(RespRequest request, Participant participant, String command) {
        this.request = request;
        this.participant = participant;
        this.command = command;
    }

    @Override
    public RespResponse call() throws Exception {
        String s = null;
        switch(command){
            case "SET":{
                s = HttpClientUtil.doPostJson("http://"+participant.getIp()+":"+participant.getPort()+"/participant/set",
                        JSONObject.toJSONString(request));
                break;
            }
            case "GET":{
                s = HttpClientUtil.doPostJson("http://"+participant.getIp()+":"+participant.getPort()+"/participant/get",
                        JSONObject.toJSONString(request));
                break;
            }
            case "DEL":{
                s = HttpClientUtil.doPostJson("http://"+participant.getIp()+":"+participant.getPort()+"/participant/del",
                        JSONObject.toJSONString(request));
                break;
            }
            case "COMMIT":{
                s = HttpClientUtil.doPostJson("http://"+participant.getIp()+":"+participant.getPort()+"/participant/commit",
                        JSONObject.toJSONString(request));
                break;
            }
            case "ROLLBACK":{
                s = HttpClientUtil.doPostJson("http://"+participant.getIp()+":"+participant.getPort()+"/participant/rollback",
                        JSONObject.toJSONString(request));
                break;
            }
            default:
                return null;
        }
        return JSON.parseObject(s,RespResponse.class);

    }
}
