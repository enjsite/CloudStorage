package net.enjy.cloudstorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.enjy.cloudstorage.common.AuthMessage;
import net.enjy.cloudstorage.common.AuthOk;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private boolean authorized;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!authorized) {
            if (msg instanceof AuthMessage) {
                AuthMessage authMessage = (AuthMessage) msg;
                if (authMessage.getLogin().equals("login") && (authMessage.getPassword().equals("password"))) {
                    String username = authMessage.getLogin();
                    authorized = true;
                    System.out.println("Authorized client");
                    AuthOk authOk = new AuthOk();
                    ctx.writeAndFlush(authOk);//.await();
                    ctx.pipeline().addLast(new MainHandler(username));
                }
            } else {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}
