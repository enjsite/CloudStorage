package net.enjy.cloudstorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.enjy.cloudstorage.common.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private String username;

    public MainHandler(String username) {
        this.username = username;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get("server_storage/" +username+ "/" + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get("server_storage/" +username+ "/" + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get("server_storage/" +username+ "/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                // если получили и сохранили файл на сервере - отправляем клиенту обновленный список файлов
                FileListMessage fl = new FileListMessage(Paths.get("server_storage/" +username+ "/"));
                ctx.writeAndFlush(fl);
            }
            if (msg instanceof FileDeleteRequest) {
                FileDeleteRequest fdr = (FileDeleteRequest) msg;
                Files.deleteIfExists(Paths.get("server_storage/" +username+ "/" + fdr.getFilename()));
                // если удалили файл на сервере - отправляем клиенту обновленный список файлов
                FileListMessage fl = new FileListMessage(Paths.get("server_storage/" +username+ "/"));
                ctx.writeAndFlush(fl);
            }
            if (msg instanceof FileListRequest) {
                //FileListMessage fl = new FileListMessage(Paths.get("server_storage/" +username+ "/"));
                //ctx.writeAndFlush(fl);
                sendFileListMessage(ctx);
            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    public void sendFileListMessage(ChannelHandlerContext ctx) throws IOException {
        FileListMessage fl = new FileListMessage(Paths.get("server_storage/" +username+ "/"));
        ctx.writeAndFlush(fl);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
