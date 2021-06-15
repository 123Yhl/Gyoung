package cn.yhl.kvstore2pcsystem;


import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.Participant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

public class Config {
    //单例模式
    private static Config config;
    private String coo_IP;
    private String coo_PORT;
    private String Server_PORT; //netty服务端端口号
    private LinkedList<Participant> participants;
    private int mode;   //0 协调者 1 参与者

    private Config(){
        participants = new LinkedList<>();
    }

    public static Config getConfig(){
        if(config == null){
            config = new Config();
        }
        return config;
    }

    public void LoadConfigFile(String filepath) throws IOException {
        File file = new File(filepath);
        if(file.exists()){
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String str;
            while ((str=reader.readLine()) != null){
                    if(str.indexOf("server_netty")!=-1){
                        String[] s = str.split(" ");
                        if(s.length!=2){
                            System.out.println("配置文件格式错误1！！！");
                        }else{
                            Server_PORT = s[1];
                        }
                    }else if(str.indexOf("participant_info")!=-1){
                        String substring = str.substring(str.indexOf(" ") + 1);
                        String[] split = substring.split(":");
                        Participant participant = new Participant();
                        participant.setIp(split[0]);
                        participant.setPort(split[1]);
                        participants.add(participant);
                    }else if(str.indexOf("mode")!=-1) {
                        String substring = str.substring(str.indexOf(" ") + 1);
                        if(substring.equals("coordinator")){
                            mode = 0;
                        }else if(substring.equals("participant")){
                            mode = 1;
                        }else{
                            System.out.println("配置文件格式错误2！！！");
                        }
                    }
                    else if(str.indexOf("coordinator_info")!=-1){
                        String[] split = str.substring(str.indexOf(" ") + 1).split(":");
                        coo_IP = split[0];
                        coo_PORT = split[1];
                    }else{
                        //System.out.println("配置文件格式错误3！！！");
                    }

            }
        }else{
            System.out.println("无法获取配置文件");
        }
    }

    public static void setConfig(Config config) {
        Config.config = config;
    }

    public String getCoo_IP() {
        return coo_IP;
    }

    public void setCoo_IP(String coo_IP) {
        this.coo_IP = coo_IP;
    }

    public String getCoo_PORT() {
        return coo_PORT;
    }

    public void setCoo_PORT(String coo_PORT) {
        this.coo_PORT = coo_PORT;
    }

    public String getServer_PORT() {
        return Server_PORT;
    }

    public void setServer_PORT(String server_PORT) {
        Server_PORT = server_PORT;
    }

    public LinkedList<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(LinkedList<Participant> participants) {
        this.participants = participants;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }


    @Override
    public String toString() {
        return "Config{" +
                "coo_IP='" + coo_IP + '\'' +
                ", coo_PORT='" + coo_PORT + '\'' +
                ", Server_PORT='" + Server_PORT + '\'' +
                ", participants=" + participants +
                ", mode=" + mode +
                '}';
    }

//    public static void main(String[] args) throws IOException {
//        String filename = "F:\\Labs\\kvstore2pcsystem\\src\\main\\resources\\coordinator.conf";
//        Config config = new Config();
//        config.LoadConfigFile(filename);
//        System.out.println(config.toString());
//    }

}
