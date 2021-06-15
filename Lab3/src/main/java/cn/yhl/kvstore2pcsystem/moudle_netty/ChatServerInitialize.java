package cn.yhl.kvstore2pcsystem.moudle_netty;

import cn.yhl.kvstore2pcsystem.moudle_coordinator.coordinatorCondition;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Conditional({coordinatorCondition.class})
@Component
public class ChatServerInitialize extends ChannelInitializer<SocketChannel> {
    @Resource
    private ChatServerHandler chatServerHandler;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        //pipeline.addLast("frame", new DelimiterBasedFrameDecoder(1024, Unpooled.wrappedBuffer("#%_$".getBytes())));
        pipeline.addLast("decode", new StringDecoder());//解码器
        pipeline.addLast("encode", new StringEncoder());
        pipeline.addLast("handler", chatServerHandler);
    }
}
