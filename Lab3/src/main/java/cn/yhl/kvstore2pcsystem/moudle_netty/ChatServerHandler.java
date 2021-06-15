package cn.yhl.kvstore2pcsystem.moudle_netty;


import cn.yhl.kvstore2pcsystem.moudle_coordinator.coordinatorCondition;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.NodeManager;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.Participant;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.RequestToParticipant;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespRequest;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespResponse;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.Task;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.util.ParseUtils;
import io.netty.channel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


@Conditional({coordinatorCondition.class})
@Component
@ChannelHandler.Sharable
public class ChatServerHandler extends SimpleChannelInboundHandler<String> {
    private ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    NodeManager manager;
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        Channel client = channelHandlerContext.channel();
        Task task = new Task(s, client);
        RespRequest req = null;
        if(manager.modcount==0){
            writeBackError(client);
            return;
        }
        try {
            req = ParseUtils.parse(task.getRequest_line());
        } catch (Exception e) {
            System.out.println("解析指令异常！！！");
        }
        req.setTransaction_ID(task.getTransaction_ID());
        if (req == null) {
            System.out.println("[ERROR]请求解析错误：" + task.getRequest_line());
        } else {
            req.setTransaction_ID(task.getTransaction_ID());
            if (req.getMethod().equals("GET")) {
                do_GET(req,client);
            } else if (req.getMethod().equals("SET")) {
                do_SET(req,client);
            } else {
                do_DEL(req,client);
            }
        }
        System.out.println("DEBUG:接收到请求：" + s);
    }

    //SET的二阶段逻辑
    private void do_SET(RespRequest request,Channel client) {
        //第一阶段
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        //futures相当于订单，无阻塞的分发任务之后，阻塞等待返回结果
        HashMap<Participant, FutureTask<RespResponse>> futures = new HashMap<>();
        for (Participant participant : aliveParticipantList) {
            FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "SET"));
            futures.put(participant, futureTask);
            executor.submit(futureTask);
        }
        boolean abort = false;
        try {
            //检查所有参与者的返回情况
            for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                RespResponse respResponse = entry.getValue().get();
                // 检查prepare-request的响应结果
                if (respResponse == null || respResponse.getResponseType() != RespResponse.SET_OK) {//检查返回结果
                    //SET的响应结果不是OK或者数据节点挂了 那么Abort这次任务
                    aliveParticipantList.remove(entry.getKey());
                    abort = false;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.out.println("有参与者宕机~本次请求回滚！");
        }
        //一阶段不成功时，回滚
        if (abort) {
            futures = new HashMap();
            for (Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "ROLLBACK"));
                futures.put(participant, futureTask);
                executor.submit(futureTask);
            }
            boolean isDone = false;
            try {
                //检查所有参与者的返回情况
                for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                    RespResponse respResponse = entry.getValue().get();
                    // 检查commit的响应结果
                    if (respResponse == null || respResponse.getResponseType() != RespResponse.SET_ROLLBACK_DONE) {
                        //数据节点挂了 或者 响应状态不是SET_ROLLBACK_DONE（这种情况理论上来说不可能出现）直接无视
                        continue;
                    } else {
                        //响应结果为SET_ROLLBACK_DONE
                        //只要有一个节点DONE了 说明最终结果是done 要做的只是等待所有响应结果都确定下来（要么done 要么挂掉）
                        isDone = true;
                        //取最后一个有效的respResponse
                        //standardResponse = respResponse;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (isDone) {
                //回滚成功,SET不成功
                writeBackError(client);
            } else {
                //子节点全部挂掉
                writeBackError(client);
            }
        } else { //第二阶段
            futures = new HashMap();
            for (Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "COMMIT"));
                futures.put(participant, futureTask);
                executor.submit(futureTask);
            }
            boolean isDone = false;
            RespResponse standardResponse = null;
            try {
                //检查所有参与者的返回情况
                for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                    RespResponse respResponse = entry.getValue().get();
                    // 检查commit的响应结果
                    if (respResponse == null || respResponse.getResponseType() != RespResponse.SET_COMMIT_DONE) {
                        //数据节点挂了 或者 响应状态不是SET_ROLLBACK_DONE（这种情况理论上来说不可能出现）直接无视
                        continue;
                    } else {
                        isDone = true;
                        //取最后一个有效的respResponse
                        standardResponse = respResponse;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (isDone) {
                //事务提交成功
                writeBack(standardResponse,client);
            } else {
                //子节点全部挂掉返回错误
                writeBackError(client);
            }
        }
    }

    //DEL
    private void do_DEL(RespRequest request,Channel client) {
        //第一阶段
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        //futures相当于订单，无阻塞的分发任务之后，阻塞等待返回结果
        HashMap<Participant, FutureTask<RespResponse>> futures = new HashMap<>();
        for (Participant participant : aliveParticipantList) {
            FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "DEL"));
            futures.put(participant, futureTask);
            executor.submit(futureTask);
        }
        boolean abort = false;
        try {
            //检查所有参与者的返回情况
            for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                RespResponse respResponse = entry.getValue().get();
                // 检查prepare-request的响应结果
                if (respResponse == null || respResponse.getResponseType() != RespResponse.DEL_OK) {//检查返回结果
                    //SET的响应结果不是OK或者数据节点挂了 那么Abort这次任务
                    aliveParticipantList.remove(entry.getKey());
                    abort = true;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.out.println("有参与者宕机 本次请求回滚");
        }
        //一阶段不成功时
        if (abort) {
            futures = new HashMap();
            for (Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "ROLLBACK"));
                futures.put(participant, futureTask);
                executor.submit(futureTask);
            }
            boolean isDone = false;
            try {
                //检查所有参与者的返回情况
                for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                    RespResponse respResponse = entry.getValue().get();
                    // 检查commit的响应结果
                    if (respResponse == null || respResponse.getResponseType() != RespResponse.DEL_ROLLBACK_DONE) {
                        //数据节点挂了 或者 响应状态不是SET_ROLLBACK_DONE（这种情况理论上来说不可能出现）直接无视
                        continue;
                    } else {
                        //响应结果为SET_ROLLBACK_DONE
                        //只要有一个节点DONE了 说明最终结果是done 要做的只是等待所有响应结果都确定下来（要么done 要么挂掉）
                        isDone = true;
                        //取最后一个有效的respResponse
                        //standardResponse = respResponse;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (isDone) {
                //回滚成功,SET不成功
                writeBackError(client);
            } else {
                //子节点全部挂掉
                writeBackError(client);
            }
        } else { //第二阶段
            futures = new HashMap();
            for (Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "COMMIT"));
                futures.put(participant, futureTask);
                executor.submit(futureTask);
            }
            boolean isDone = false;
            RespResponse standardResponse = null;
            try {
                //检查所有参与者的返回情况
                for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                    RespResponse respResponse = entry.getValue().get();
                    // 检查commit的响应结果
                    if (respResponse == null || respResponse.getResponseType() != RespResponse.DEL_COMMIT_DONE) {
                        //数据节点挂了 或者 响应状态不是SET_ROLLBACK_DONE（这种情况理论上来说不可能出现）直接无视
                        continue;
                    } else {
                        isDone = true;
                        //取最后一个有效的respResponse
                        standardResponse = respResponse;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (isDone) {
                //事务提交成功
                writeBack(standardResponse,client);
            } else {
                //子节点全部挂掉返回错误
                writeBackError(client);
            }
        }
    }

    void do_GET(RespRequest request,Channel client) {
        System.out.println("进入do_GET方法");
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        HashMap<Participant, FutureTask<RespResponse>> futures = new HashMap<>();
        for (Participant participant : aliveParticipantList) {
            FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request, participant, "GET"));
            futures.put(participant, futureTask);
            executor.submit(futureTask);
        }
        RespResponse lastResponse = null;
        try {
            //检查所有参与者的返回情况
            for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                RespResponse respResponse = entry.getValue().get();
                // 检查prepare-request的响应结果
                if (respResponse == null || respResponse.getResponseType() != RespResponse.GET_OK) {//检查返回结果
                    //SET的响应结果不是OK或者数据节点挂了 那么Abort这次任务
                    aliveParticipantList.remove(entry.getKey());
                } else {
                    lastResponse = respResponse;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
            //System.out.println("有参与者宕机 本次请求回滚");
        }
        if(lastResponse == null){
            writeBackError(client);
        }else{
            writeBack(lastResponse,client);
        }
    }

    private void writeBack(RespResponse response,Channel client) {
        System.out.println(response);
        StringBuffer sb = new StringBuffer();
        if (response.getResponseType() == RespResponse.DEL_COMMIT_DONE) {
            sb.append(":");
            sb.append(response.getKeysRemoved());
            sb.append("\r\n");
            //sb.append(Const.SEPARATOR);
        } else if (response.getResponseType() == RespResponse.SET_COMMIT_DONE) {
            sb.append("+OK\r\n");
            //sb.append(Const.SEPARATOR);
        } else if (response.getResponseType() == RespResponse.GET_OK) {
            if (response.getValues() == null || response.getValues().size() == 0) {
                sb.append("*1\r\n$3\r\nnil\r\n");
            } else {
                sb.append("*");
                sb.append(response.getValues().size());
                sb.append("\r\n");
                for (String value : response.getValues()) {
                    sb.append("$");
                    sb.append(value.length());
                    sb.append("\r\n");
                    sb.append(value);
                    sb.append("\r\n");
                }
                //sb.append(Const.SEPARATOR);
            }
        }
        client.writeAndFlush(sb.toString()).addListener(ChannelFutureListener.CLOSE);
    }

    private void writeBackError(Channel client) {
        System.out.println("写回错误结果！");
        client.writeAndFlush("-ERROR\r\n").addListener(ChannelFutureListener.CLOSE);
    }

}
