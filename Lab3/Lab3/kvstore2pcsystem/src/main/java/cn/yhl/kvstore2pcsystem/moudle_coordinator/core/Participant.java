package cn.yhl.kvstore2pcsystem.moudle_coordinator.core;

public class Participant {
    private String co_ip;
    private String co_port;
    private String ip;//参与者IP
    private String port;//参与者端口

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

    @Override
    public String toString() {
        return "Participant{" +
                "co_ip='" + co_ip + '\'' +
                ", co_port='" + co_port + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}
