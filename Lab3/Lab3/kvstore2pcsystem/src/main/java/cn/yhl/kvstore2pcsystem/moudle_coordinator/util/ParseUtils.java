package cn.yhl.kvstore2pcsystem.moudle_coordinator.util;



import cn.yhl.kvstore2pcsystem.moudle_coordinator.resp.RespRequest;

import java.util.LinkedList;

/**
 * @author codefriday
 * @data 2021/5/25
 */
public class ParseUtils {
    //请求类型枚举
    public static int SET = 101;      //存储
    public static int GET = 102;      //查询
    public static int DEL = 103;      //删除
    //SET请求的参数为 key value
    //GET请求的参数为 key
    //DEL请求的参数为 key1 key2 key3 ...
    public static RespRequest parse(String msg){
        //System.out.println(msg);
        if (msg == null || msg.isEmpty()) {
            return null;
        }
        RespRequest request = new RespRequest();
        String[] split = msg.split("\r\n");
        //传入参数不正确       按照\r\n划分之后 至少都是五个字符串 参数计数、method长度、method、key长度、key 少于5个就一定是错误的请求
        if (split.length < 5) {
            return null;
        }
        //首先解析第一行
        char c0 = split[0].charAt(0);
        if (c0 != '*') {
            return null;
        }
        int paramCount = 0;
        try {
            paramCount = Integer.valueOf(split[0].substring(1));

            //校验参数总长度是否正确   1 + 2 * 参数数量 = split[]的长度
            if (split.length != 1 + 2 * paramCount) {
                return null;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
        //共有paramCount这么多个参数 把split[1]开始 每两个字符串分成一组进行抽取
        //先单独提取method
        c0 = split[1].charAt(0);
        if (c0 != '$') {
            return null;
        }
        int paramLength = 0;
        try {
            paramLength = Integer.valueOf(split[1].substring(1));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
        if (split[2].length() != paramLength) {
            return null;
        }
        int i = METHOD.codeOf(split[2]);
        if (i == -1) {
            return null;
        }
        request.setMethod(split[2]);
        //SET 一个key 多个value
        if (METHOD.codeOf(request.getMethod()) == METHOD.SET.getCode()) {
            //先单独提取key
            c0 = split[3].charAt(0);
            if (c0 != '$') {
                return null;
            }
            try {
                paramLength = Integer.valueOf(split[3].substring(1));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
            if (split[4].length() != paramLength) {
                return null;
            }
            LinkedList<String> list = new LinkedList<>();
            list.add(split[4]);
            request.setKeys(list);

            //提取values
            request.setValues(new LinkedList<>());
            for (int j = 5; j < split.length; j += 2) {
                c0 = split[j].charAt(0);
                if (c0 != '$') {
                    return null;
                }
                paramLength = 0;
                try {
                    paramLength = Integer.valueOf(split[j].substring(1));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return null;
                }
                if (split[j + 1].length() != paramLength) {
                    return null;
                }
                request.getValues().add(split[j + 1]);
            }
        }

        //GET 一个key
        else if (METHOD.codeOf(request.getMethod()) == METHOD.GET.getCode()) {
            //先单独提取key
            c0 = split[3].charAt(0);
            if (c0 != '$') {
                return null;
            }
            try {
                paramLength = Integer.valueOf(split[3].substring(1));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
            if (split[4].length() != paramLength) {
                return null;
            }
            LinkedList<String> list = new LinkedList<>();
            list.add(split[4]);
            request.setKeys(list);
        }
        //DEL 多个key
        else if (METHOD.codeOf(request.getMethod()) == METHOD.DEL.getCode()) {
            //提取key
            request.setKeys(new LinkedList<>());
            for (int j = 3; j < split.length; j += 2) {
                c0 = split[j].charAt(0);
                if (c0 != '$') {
                    return null;
                }
                try {
                    paramLength = Integer.valueOf(split[j].substring(1));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return null;
                }
                if (split[j + 1].length() != paramLength) {
                    return null;
                }
                request.getKeys().add(split[j + 1]);
            }
        }
        //请求类型不支持
        else {

        }
        return request;
    }


    public enum METHOD {
        SET("SET", 101), GET("GET", 102), DEL("DEL", 103);
        private String Name;
        private int code;

        METHOD(String name, int code) {
            Name = name;
            this.code = code;
        }

        public static String nameOf(int code) {
            for (METHOD method : values()) {
                if (method.getCode() == code) {
                    return method.getName();
                }
            }
            return null;
        }

        public static int codeOf(String name) {
            for (METHOD method : values()) {
                if (method.getName().equals(name)) {
                    return method.getCode();
                }
            }
            return -1;
        }

        public static METHOD enumOf(int requestType) {
            for (METHOD method : values()) {
                if (method.getCode() == (requestType)) {
                    return method;
                }
            }
            return null;
        }


        public String getName() {
            return Name;
        }

        public void setName(String name) {
            Name = name;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }
}
