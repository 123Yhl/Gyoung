package cn.yhl.kvstore2pcsystem.moudle_coordinator.core;

import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespRequest;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespResponse;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.util.ParseUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class Server implements Runnable{
    private ExecutorService executor;
    private Socket client;

    public Server(Socket socket) {
        executor = Executors.newCachedThreadPool();
        this.client = socket;
    }
    //序列化处理任务
    @Override
    public void run() {
        RespRequest req = null;
        String msg = null;
        while(!client.isClosed()){
            try {
                msg = readDataFromSocket();
                req = ParseUtils.parse(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(req == null){
                System.out.println("[ERROR]请求解析错误："+msg);
            }else {
                req.setTransaction_ID(UUID.randomUUID().toString());
                if (req.getMethod().equals("GET")) {
                    do_GET(req);
                } else if (req.getMethod().equals("SET")) {
                    do_SET(req);
                } else {
                    do_DEL(req);
                }
            }
        }
    }
    private String readDataFromSocket() throws IOException, InterruptedException {
        // 封装数据源
        InputStream inputStream = client.getInputStream();
        int count = 0;
        while (count == 0) {
            Thread.sleep(200);
            //检查socket是否断开
            if (client.isClosed()) {
                return "soket is closed";
            }
            count = inputStream.available();
        }
        if (count != 0) {
            byte[] bt = new byte[count];
            int readCount = 0;
            while (readCount < count) {
                readCount += inputStream.read(bt, readCount, count - readCount);
            }
            return new String(bt, StandardCharsets.UTF_8);
        }
        return "";
    }

    //SET的二阶段逻辑
    private void do_SET(RespRequest request){
        //第一阶段
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        //futures相当于订单，无阻塞的分发任务之后，阻塞等待返回结果
        HashMap<Participant, FutureTask<RespResponse>> futures = new HashMap<>();
        for (Participant participant : aliveParticipantList) {
            FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"SET"));
            futures.put(participant,futureTask);
            executor.submit(futureTask);
        }
        boolean abort = false;
        try {
            //检查所有参与者的返回情况
            for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                RespResponse respResponse = entry.getValue().get();
                // 检查prepare-request的响应结果
                if (respResponse == null || respResponse.getResponseType()!=RespResponse.SET_OK) {//检查返回结果
                    //SET的响应结果不是OK或者数据节点挂了 那么Abort这次任务
                    aliveParticipantList.remove(entry.getKey());
                    abort = false;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.out.println("有参与者宕机~本次请求回滚！");
        }
        //一阶段不成功时，回滚
        if(abort){
            futures = new HashMap();
            for(Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"ROLLBACK"));
                futures.put(participant,futureTask);
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
            if(isDone){
                //回滚成功,SET不成功
                writeBackError();
            }else {
                //子节点全部挂掉
                writeBackError();
            }
        }else { //第二阶段
            futures = new HashMap();
            for(Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"COMMIT"));
                futures.put(participant,futureTask);
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
            if(isDone){
                //事务提交成功
                writeBack(standardResponse);
            }else {
                //子节点全部挂掉返回错误
                writeBackError();
            }
        }
    }

    //DEL
    private void do_DEL(RespRequest request){
        //第一阶段
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        //futures相当于订单，无阻塞的分发任务之后，阻塞等待返回结果
        HashMap<Participant, FutureTask<RespResponse>> futures = new HashMap<>();
        for (Participant participant : aliveParticipantList) {
            FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"DEL"));
            futures.put(participant,futureTask);
            executor.submit(futureTask);
        }
        boolean abort = false;
        try {
            //检查所有参与者的返回情况
            for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                RespResponse respResponse = entry.getValue().get();
                // 检查prepare-request的响应结果
                if (respResponse == null || respResponse.getResponseType()!=RespResponse.DEL_OK) {//检查返回结果
                    //SET的响应结果不是OK或者数据节点挂了 那么Abort这次任务
                    aliveParticipantList.remove(entry.getKey());
                    abort = true;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            System.out.println("有参与者宕机 本次请求回滚");
        }
        //一阶段不成功时
        if(abort){
            futures = new HashMap();
            for(Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"ROLLBACK"));
                futures.put(participant,futureTask);
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
            if(isDone){
                //回滚成功,SET不成功
                writeBackError();
            }else {
                //子节点全部挂掉
                writeBackError();
            }
        }else { //第二阶段
            futures = new HashMap();
            for(Participant participant : aliveParticipantList) {
                FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"COMMIT"));
                futures.put(participant,futureTask);
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
            if(isDone){
                //事务提交成功
                writeBack(standardResponse);
            }else {
                //子节点全部挂掉返回错误
                writeBackError();
            }
        }
    }

    void do_GET(RespRequest request){
        System.out.println("进入do_GET方法");
        ArrayList<Participant> aliveParticipantList = NodeManager.getAliveParticipantList();
        HashMap<Participant, FutureTask<RespResponse>> futures = new HashMap<>();
        for (Participant participant : aliveParticipantList) {
            FutureTask<RespResponse> futureTask = new FutureTask(new RequestToParticipant(request,participant,"GET"));
            futures.put(participant,futureTask);
            executor.submit(futureTask);
        }
        RespResponse lastResponse = null;
        try {
            //检查所有参与者的返回情况
            for (Map.Entry<Participant, FutureTask<RespResponse>> entry : futures.entrySet()) {
                RespResponse respResponse = entry.getValue().get();
                // 检查prepare-request的响应结果
                if (respResponse == null || respResponse.getResponseType()!=RespResponse.GET_OK) {//检查返回结果
                    //SET的响应结果不是OK或者数据节点挂了 那么Abort这次任务
                    aliveParticipantList.remove(entry.getKey());
                }else{
                    lastResponse = respResponse;
                }
            }

        } catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
            System.out.println("有参与者宕机 本次请求回滚");
        }
        if(lastResponse == null) writeBackError();
        writeBack(lastResponse);
    }
    private void writeBack(RespResponse response){
        System.out.println(response);
        StringBuffer sb = new StringBuffer();
        if(response.getResponseType()==RespResponse.DEL_COMMIT_DONE){
            sb.append(":");
            sb.append(response.getKeysRemoved());
            sb.append("\r\n");
            //sb.append(Const.SEPARATOR);
        }else if(response.getResponseType()==RespResponse.SET_COMMIT_DONE){
            sb.append("+OK\r\n");
            //sb.append(Const.SEPARATOR);
        }else if(response.getResponseType()==RespResponse.GET_OK){
            if(response.getValues()==null || response.getValues().size()==0){
                sb.append("*1\r\n$3\r\nnil\r\n");
            }else{
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
        try {
            OutputStream outputStream = client.getOutputStream();
            PrintWriter pWriter = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
            pWriter.println(sb.toString());
            pWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("执行成功，进入写回结果:"+sb.toString());
    }

    private void writeBackError(){
        try {
            OutputStream outputStream = client.getOutputStream();
            String s = "-ERROR\r\n";
            PrintWriter pWriter = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
            pWriter.println(s);
            pWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
