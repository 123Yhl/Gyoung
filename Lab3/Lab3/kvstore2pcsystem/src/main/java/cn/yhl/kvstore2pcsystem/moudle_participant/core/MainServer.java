package cn.yhl.kvstore2pcsystem.moudle_participant.core;

import cn.yhl.kvstore2pcsystem.moudle_participant.participantCondition;
import cn.yhl.kvstore2pcsystem.moudle_participant.resp.RespRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Conditional({participantCondition.class})
@Service
public class MainServer {
    @Autowired
    Participant participant;
    private DB database;
    private HashMap<String, RespRequest> log;
    public MainServer(){
        database = new DB();
        log = new HashMap<>();
        //init();
    }

    public boolean SET_DEL_ONE_STAGE(RespRequest request){
        if(request == null) return false;
        log.put(request.getTransaction_ID(),request);
        return true;
    }

    public boolean COMMIT(RespRequest request){
        if(request == null) return false;
        if(log.get(request.getTransaction_ID())==null) return false;
        log.remove(request.getTransaction_ID());
        if(request.getMethod().equals("SET")){
            //System.out.println(request);
            StringBuffer val = new StringBuffer();
            for (String value : request.getValues()) {
                val.append(value);
                val.append(" ");
            }
            val.deleteCharAt(val.length()-1);
            if(request.getKeys().size()!=1) return false;
            //先删除再重写
            database.del(request.getKeys());
            database.set(request.getKeys().getFirst(),val.toString());
            return true;
        }else if(request.getMethod().equals("DEL")){
            return database.del(request.getKeys());
        }
        return false;
    }

    public boolean ROLLBACK(RespRequest request){
        if(request == null) return false;
        if(log.get(request.getTransaction_ID()) == null) return false;
        log.remove(request.getTransaction_ID());
        return true;
    }

    public ArrayList<String> GET(String key){
        if(key==null) return null;
        ArrayList<String> values = new ArrayList<>();
        String s = database.get(key);
        if(s == null) return null;
        String[] s1 = s.split(" ");
        for (String s2 : s1) {
            values.add(s2);
        }
        return values;
    }

    public int getRemoveCnt(){
        return database.getRemoveCnt();
    }

    public DB getDatabase() {
        return database;
    }
}
