package cn.yhl.kvstore2pcsystem.moudle_participant.core;

import cn.yhl.kvstore2pcsystem.moudle_participant.participantCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;

@Conditional({participantCondition.class})
@Component
public class DB {
    private int removeCnt = 0;
    public HashMap<String, String> database;
    //private Integer size;

    public DB() {
        database = new HashMap<>();
    }

    public void set(String key, String value) {
        database.put(key, value);
    }

    /*
        没有key时返回null
     */
    public String get(String key) {
        return database.get(key);
    }

    public boolean del(LinkedList<String> keys) {
        int cnt = 0;
        for (String key : keys) {
            if (database.remove(key) != null)
                cnt++;
        }
        removeCnt = cnt;
        return true;
    }

    public int getRemoveCnt() {
        return removeCnt;
    }

}
