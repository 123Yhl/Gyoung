package cn.yhl.kvstore2pcsystem;

import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.NodeManager;
import cn.yhl.kvstore2pcsystem.moudle_coordinator.core.Participant;
import cn.yhl.kvstore2pcsystem.moudle_netty.NettyServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Date;

@SpringBootApplication
public class Kvstore2pcsystemApplication implements CommandLineRunner {

    @Autowired(required = false)
    NettyServer nettyServer;

    @Autowired(required = false)
    cn.yhl.kvstore2pcsystem.moudle_participant.core.Participant participant;

    public static void main(String[] args) throws IOException {
        String filename = args[1];
        Config config = Config.getConfig();
        config.LoadConfigFile(filename);
        if(config.getMode()==1){
            String str = "--server.port="+config.getParticipants().getFirst().getPort();
            String str1 = "--server.address="+config.getParticipants().getFirst().getIp();
            String[] args1 = {str,str1};
            SpringApplication.run(Kvstore2pcsystemApplication.class, args1);
        }
        else{
            SpringApplication.run(Kvstore2pcsystemApplication.class, args);
        }
    }

    @Override
    public void run(String... args1) throws Exception {
        Config config = Config.getConfig();
        if(config.getMode()==0){
            System.out.println("====>"+config.getParticipants().size());
            for (Participant participant : config.getParticipants()) {
                NodeManager.participants.put(participant,new Date());
            }
            nettyServer.start(config.getCoo_IP(),Integer.parseInt(config.getCoo_PORT()));
        }else{
            participant.setCo_port(config.getCoo_PORT());
            participant.setCo_addr(config.getCoo_IP());
            participant.setIp(config.getParticipants().getFirst().getIp());
            participant.setPort(config.getParticipants().getFirst().getPort());
        }
    }
}
