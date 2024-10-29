package com.example;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

public class AudioWebSocket extends BinaryWebSocketHandler {
    @Autowired
    private WebSocketClient client; // 重用的WebSocketClient实例

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Connected: " + session.getId());
        try {
            client = new WebSocketClient(new URI("ws://localhost:8080/api/")); // 创建客户端// 确保连接建立
            // 确保 WebSocketClient 在这里成功连接
            System.out.println("WebSocketClient initialized.");
        } catch (Exception e) {
            System.err.println("Failed to create WebSocketClient: " + e.getMessage());
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] audioData = message.getPayload().array();

        // 如果接收到关闭信号
        String msg = new String(audioData);
        if (msg.contains("CLOSE_MIC")) {    
             // 关闭连接
            session.close();
            System.out.println("Disconnected: " + session.getId());
            return;
         }

         if (client !=null){
        String transcription = sendAudioToPythonServer(audioData);        
        
        // 将转录结果发送回客户端
        try{
            synchronized(session){
                session.sendMessage(new TextMessage(transcription));
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Disconnected: " + session.getId());
        if (client != null) {
            client.close(); // 关闭WebSocketClient连接
        }
    }

    private String sendAudioToPythonServer(byte[] audioData) {
        // 实现与 Python WebSocket 服务器的连接并发送音频数据
        try {
            client.send(audioData);         
            
            String transcription = client.getTranscription();           

            return transcription;
            
        } catch (Exception e) {
            e.printStackTrace();
            return "Error connecting to Python server";
        }
    }
}