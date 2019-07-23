package net.enjy.cloudstorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.enjy.cloudstorage.common.FileListMessage;
import net.enjy.cloudstorage.common.FileListRequest;
import net.enjy.cloudstorage.common.FileMessage;
import net.enjy.cloudstorage.common.FileRequest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MainHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server_storage/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server_storage/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);

                // если получили и сохранили файл на сервере - отправляем клиенту обновленный список файлов
                FileListMessage fl = new FileListMessage(Paths.get("server_storage/"));
                ctx.writeAndFlush(fl);
            }
            if (msg instanceof FileListRequest) {
                FileListMessage fl = new FileListMessage(Paths.get("server_storage/"));
                ctx.writeAndFlush(fl);
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
