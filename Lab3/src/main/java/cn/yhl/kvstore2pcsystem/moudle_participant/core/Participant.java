package cn.yhl.kvstore2pcsystem.moudle_participant.core;


import cn.yhl.kvstore2pcsystem.moudle_participant.participantCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional({participantCondition.class})
@Component
public class Participant {
    private String co_addr; //协调者地址
    private String co_port; //协调者接口ip
    private String ip;//参与者IP
    private String port;//参与者端口

    public String getCo_addr() {
        return co_addr;
    }

    public void setCo_addr(String co_addr) {
        this.co_addr = co_addr;
    }

    public String getCo_port() {
        return co_port;
    }

    public void setCo_port(String co_port) {
        this.co_port = co_port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}
