package net.enjy.cloudstorage.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import net.enjy.cloudstorage.common.AuthMessage;
import net.enjy.cloudstorage.common.AuthOk;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private boolean authorized;
    public Map<String, String> users = new HashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!authorized) {
            if (msg instanceof AuthMessage) {
                AuthMessage authMessage = (AuthMessage) msg;

                try{
                    Class.forName("org.sqlite.JDBC");

                    try (Connection connection = DriverManager.getConnection("jdbc:sqlite:clients.db")) {
                        Statement statement = connection.createStatement();

                        for(Client client : readAllClients(statement)) {
                            System.out.println(client);
                            users.put(client.getLogin(), client.getPassword());
                        }

                    }catch (SQLException ex){
                        System.out.println(ex.getMessage());
                    }
                }catch (ClassNotFoundException e){
                    System.out.println(e.getMessage());
                }

                if (authUser(authMessage.getLogin(), authMessage.getPassword()))  {
                    String username = authMessage.getLogin();
                    authorized = true;
                    System.out.println("Authorized client");

                    // проверяем, существует ли папка юзера, если нет - создаем
                    if (Files.notExists(Paths.get("server_storage/" +username+ "/"))) {
                        Files.createDirectory(Paths.get("server_storage/" +username+ "/"));
                    }

                    AuthOk authOk = new AuthOk();
                    ctx.writeAndFlush(authOk);
                    ctx.pipeline().addLast(new MainHandler(username));
                }
            } else {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private static Collection<Client> readAllClients(Statement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery("SELECT * FROM clients");

        Map<Integer, Client> clientById = new HashMap<>();
        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            if (clientById.get(id) == null) {
                clientById.put(id, createClient(resultSet, id));
            }
        }
        return clientById.values();
    }

    private static Client createClient(ResultSet resultSet, int id) throws SQLException {
        String login = resultSet.getString(2);
        String password = resultSet.getString(3);

        Client client = new Client();
        client.setId(id);
        client.setLogin(login);
        client.setPassword(password);
        return client;
    }

    public boolean authUser(String username, String password) {
        String pwd = users.get(username);
        return pwd != null && pwd.equals(password);
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
