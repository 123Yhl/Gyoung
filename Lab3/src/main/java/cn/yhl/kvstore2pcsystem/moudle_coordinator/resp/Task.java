package cn.yhl.kvstore2pcsystem.moudle_coordinator.resp;

import io.netty.channel.Channel;

import java.util.UUID;

public class Task {

    private String Request_line;        //传过来的请求报文
    private String Transaction_ID;      //唯一的事务号
    private Channel channel;            //客户端通道

    public Task(String msg, Channel client) {
        this.Request_line = msg;
        this.channel = client;
        this.Transaction_ID = UUID.randomUUID().toString();
    }


    public String getRequest_line() {
        return Request_line;
    }

    public void setRequest_line(String request_line) {
        Request_line = request_line;
    }

    public String getTransaction_ID() {
        return Transaction_ID;
    }

    public void setTransaction_ID(String transaction_ID) {
        Transaction_ID = transaction_ID;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "Task{" +
                "Request_line='" + Request_line + '\'' +
                ", Transaction_ID='" + Transaction_ID + '\'' +
                ", channel=" + channel +
                '}';
    }
}
