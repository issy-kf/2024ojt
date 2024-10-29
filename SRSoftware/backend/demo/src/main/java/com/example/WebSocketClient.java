package com.example;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

@ClientEndpoint // 表示这是一个 WebSocket 客户端
public class WebSocketClient {

    private Session session;
    private String transcription;

    public WebSocketClient(URI endpointURI) throws Exception {
        System.out.println("Attempting to connect to WebSocket server at: " + endpointURI);
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    try {
        this.session = container.connectToServer(this, endpointURI);
        System.out.println("WebSocket connection established.");
    } catch (Exception e) {
        System.err.println("Failed to establish WebSocket connection: " + e.getMessage());
        throw e;
    }
    }

    public synchronized void send(byte[] audioData) {
        // 发送音频数据到 Python 服务器
    if (this.session != null) {
        if (this.session.isOpen()) {
            try{
            this.session.getAsyncRemote().sendBinary(ByteBuffer.wrap(audioData));
            System.out.println("Audio data sent to server.");
            }catch(Exception e){
                System.err.println("Error sending audio data: " + e.getMessage());
            }
        } else {
            System.err.println("WebSocket session is closed.");
            throw new IllegalStateException("WebSocket session is closed.");
        }
    } else {
        System.err.println("WebSocket session is not initialized.");
        throw new IllegalStateException("WebSocket session is not open or initialized.");
    }
    }

    @OnMessage
    public void onMessage(String message) {
        if ("ping".equals(message)) {
            try {
                session.getBasicRemote().sendText("pong");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 处理从 Python 服务器返回的转录结果
        this.transcription = message;
    }

    public String getTranscription() {
        return transcription;
    }

    public void close() throws Exception {
        if (this.session != null && this.session.isOpen()) {
            this.session.close();
        }
    }
}

