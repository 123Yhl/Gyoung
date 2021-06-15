package cn.yhl.kvstore2pcsystem.moudle_participant.resp;

import java.util.ArrayList;

public class RespResponse {
    public static final int SET_OK = 201;           //SET成功
    public static final int GET_OK = 202;           //GET成功
    public static final int DEL_OK = 203;           //GET成功
    public static final int ERROR = 204;           //发生错误
    public static final int SET_COMMIT_DONE = 205;             // SET命令的commit指令执行完成
    public static final int SET_ROLLBACK_DONE = 206;           // SET命令的rollback指令执行完成
    public static final int DEL_COMMIT_DONE = 207;             // DEL命令的commit指令执行完成
    public static final int DEL_ROLLBACK_DONE = 208;           // DEL命令的rollback指令执行完成
    private int responseType;                   //响应类型
    private String res;                 //需要传回的消息
    private int keysRemoved;                    //DEL请求时删除的key数量
    private ArrayList<String> keys;             //keys
    private ArrayList<String> values;            //values

    public int getResponseType() {
        return responseType;
    }

    public void setResponseType(int responseType) {
        this.responseType = responseType;
    }

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public int getKeysRemoved() {
        return keysRemoved;
    }

    public void setKeysRemoved(int keysRemoved) {
        this.keysRemoved = keysRemoved;
    }

    public ArrayList<String> getKeys() {
        return keys;
    }

    public void setKeys(ArrayList<String> keys) {
        this.keys = keys;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }
}
