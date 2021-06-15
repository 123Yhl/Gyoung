package cn.yhl.kvstore2pcsystem.moudle_participant.controller;

import cn.yhl.kvstore2pcsystem.moudle_participant.core.MainServer;
import cn.yhl.kvstore2pcsystem.moudle_participant.participantCondition;
import cn.yhl.kvstore2pcsystem.moudle_participant.resp.RespRequest;
import cn.yhl.kvstore2pcsystem.moudle_participant.resp.RespResponse;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;

@Conditional({participantCondition.class})
@RestController
@RequestMapping("/participant")
public class ParticipantController {

    @Autowired
    MainServer server;

    @PostMapping(value = "set")
    public String SetOneStage(@RequestBody JSONObject jsonObject){
        RespResponse response = new RespResponse();
        System.out.println("[DEBUG]访问SET接口");
        RespRequest respRequest = jsonObject.toJavaObject(RespRequest.class);
        if(!respRequest.getMethod().equals("SET")){
            response.setResponseType(RespResponse.ERROR);
        }else {
            if(server.SET_DEL_ONE_STAGE(respRequest)){
                response.setResponseType(RespResponse.SET_OK);
            }else{
                response.setResponseType(RespResponse.ERROR);
            }
        }
        return JSONObject.toJSONString(response);
    }

    @PostMapping(value = "del")
    public String DelOneStage(@RequestBody JSONObject jsonObject){
        RespResponse response = new RespResponse();
        System.out.println("[DEBUG]访问del接口");
        RespRequest respRequest = jsonObject.toJavaObject(RespRequest.class);
        if(!respRequest.getMethod().equals("DEL")){
            response.setResponseType(RespResponse.ERROR);
        }else {
            if(server.SET_DEL_ONE_STAGE(respRequest)){
                response.setResponseType(RespResponse.DEL_OK);
            }else{
                response.setResponseType(RespResponse.ERROR);
            }
        }
        return JSONObject.toJSONString(response);
    }
    @PostMapping(value = "commit")
    public String Commit(@RequestBody JSONObject jsonObject){
        RespResponse response = new RespResponse();
        System.out.println("[DEBUG]访问commit接口");
        RespRequest respRequest = jsonObject.toJavaObject(RespRequest.class);
        if(!respRequest.getMethod().equals("DEL") && !respRequest.getMethod().equals("SET")){
            response.setResponseType(RespResponse.ERROR);
        }else{
            if(server.COMMIT(respRequest)){
                if(respRequest.getMethod().equals("SET")){
                    response.setResponseType(RespResponse.SET_COMMIT_DONE);
                }else if(respRequest.getMethod().equals("DEL")){
                    response.setKeysRemoved(server.getRemoveCnt());
                    response.setResponseType(RespResponse.DEL_COMMIT_DONE);
                }
            }else{
                response.setResponseType(RespResponse.ERROR);
            }
        }
        return JSONObject.toJSONString(response);
    }

    @PostMapping(value = "rollback")
    public String RollBack(@RequestBody JSONObject jsonObject){
        RespResponse response = new RespResponse();
        System.out.println("[DEBUG]rollback接口");
        RespRequest respRequest = jsonObject.toJavaObject(RespRequest.class);
        if(!respRequest.getMethod().equals("DEL") && !respRequest.getMethod().equals("SET")){
            response.setResponseType(RespResponse.ERROR);
        }else{
            if(server.ROLLBACK(respRequest)){
                if(respRequest.getMethod().equals("SET")){
                    response.setResponseType(RespResponse.SET_ROLLBACK_DONE);
                }else if(respRequest.getMethod().equals("DEL")){
                    response.setResponseType(RespResponse.DEL_ROLLBACK_DONE);
                }
            }else{
                response.setResponseType(RespResponse.ERROR);
            }
        }
        return JSONObject.toJSONString(response);
    }

    @PostMapping(value = "get")
    public String GetKey(@RequestBody JSONObject jsonObject){
        RespResponse response = new RespResponse();
        System.out.println("[DEBUG]get接口");
        RespRequest respRequest = jsonObject.toJavaObject(RespRequest.class);
        if(respRequest.getKeys().size()!=1){
            response.setResponseType(RespResponse.ERROR);
        }else {
            ArrayList<String> values = server.GET(respRequest.getKeys().getFirst());
            response.setResponseType(RespResponse.GET_OK);
            response.setValues(values);
        }
        return JSONObject.toJSONString(response);
    }

    @PostMapping(value = "copy")
    public String CopyMap(HttpServletRequest request){
        String ip = request.getParameter("ip");
        String port = request.getParameter("port");
        System.out.println(ip+port+"请求复制数据库！");
        return JSONObject.toJSONString(server.getDatabase());
    }



}
