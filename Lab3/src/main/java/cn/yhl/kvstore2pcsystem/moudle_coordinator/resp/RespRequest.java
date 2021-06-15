package cn.yhl.kvstore2pcsystem.moudle_coordinator.resp;

import java.util.LinkedList;

public class RespRequest {
    private String Transaction_ID;      //唯一的事务号
    private String method;              //请求方法
    private LinkedList<String> keys;    //键
    private LinkedList<String> values;               //值

    public String getTransaction_ID() {
        return Transaction_ID;
    }

    public void setTransaction_ID(String transaction_ID) {
        Transaction_ID = transaction_ID;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public LinkedList<String> getKeys() {
        return keys;
    }

    public void setKeys(LinkedList<String> keys) {
        this.keys = keys;
    }

    public LinkedList<String> getValues() {
        return values;
    }

    public void setValues(LinkedList<String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "RespRequest{" +
                "Transaction_ID='" + Transaction_ID + '\'' +
                ", method='" + method + '\'' +
                ", keys=" + keys +
                ", values=" + values +
                '}';
    }
}
