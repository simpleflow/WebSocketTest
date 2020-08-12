import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;
//import net.sf.json.JSONObject;
/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 * 即 @ServerEndpoint 可以把当前类变成websocket服务类
 */
//访问服务端的url地址
//@ServerEndpoint(value = "/websocket/{username}")
@ServerEndpoint(value = "/websocket")
@Component
public class WebSocket {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    //concurrent包的线程安全Map，用来存放每个客户端对应的WebSocket对象。若要实现服务端与单一客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();

    private static Map<String, String> unidlist = new ConcurrentHashMap<String, String>();
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    //当前发消息的客户端的编号
    private String username;

    public static synchronized Map<String, WebSocket> getClients() {
        return clients;
    }

    public static synchronized Map<String, String> getUnidlist() {
        return unidlist;
    }

    /**
     * 连接成功后调用的方法
     * @param session  可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */

    @OnOpen
    public void onOpen(Session session,@PathParam("username") String username) throws IOException {
        this.username = username;
        this.session = session;
        //在线数加1
        addOnlineCount();
        clients.put(username, this);
        System.out.println("open websocket...online " + getOnlineCount());
    }




    @OnClose
    public void onClose() throws IOException {
        clients.remove(username);
        //在线数减1
        subOnlineCount();
        System.out.println("close websocket...online " + getOnlineCount());
    }

    //接收到客户端消息
    @OnMessage
    public void onMessage(String message,Session session) throws IOException {
        session.getBasicRemote().sendText(message);
        /*
        JSONObject jsonTo = JSONObject.fromObject(message);
        if (!jsonTo.get("To").equals("All")){
            sendMessageTo("给一个人", jsonTo.get("To").toString());
        }else{
            sendMessageAll("给所有人");
        }
        */
    }
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("websocket  ---onError");
        error.printStackTrace();
    }

    /**
     * 这个方法与上面几个方法不一样。没有用注解，是根据自己需要添加的方法。
     * @param message
     * @throws IOException
     */
    public void sendMessage(String message) throws IOException{
        this.session.getBasicRemote().sendText(message);
        //this.session.getAsyncRemote().sendText(message);
    }

    /**
     * 给指定的人发送消息
     * @param message
     */
    public void sendToUser(String tousername, String message) {
        try {
            WebSocket webSocket = clients.get(tousername) ;
            if ( webSocket != null ) {
                webSocket.sendMessage("用户" + username + "发来消息：" + message);
            } else {
                System.out.println("当前接收信息的用户不在线");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给所有人发消息
     * @param message
     */
    public void sendToAll(String message) {
        //遍历HashMap
        for (String key : clients.keySet()) {
            try {
                //判断接收用户是否是当前发消息的用户
                if ( !username.equals(key) ) {
                    clients.get(key).sendMessage("用户" + username + "发来消息：" + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }
    public static synchronized void addOnlineCount() {
        WebSocket.onlineCount++;
    }
    public static synchronized void subOnlineCount() {
        WebSocket.onlineCount--;
    }

}