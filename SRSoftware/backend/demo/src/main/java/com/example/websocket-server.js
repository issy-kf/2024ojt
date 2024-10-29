// 这是服务器端的 Node.js 文件，比如 websocket-server.js
const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', (ws) => {
  console.log('客户端已连接');

  ws.on('message', (message) => {
    console.log('收到消息:', message);

    if (message === 'ping') {
      ws.send('pong');  // 服务器收到心跳请求“ping”，并返回“pong”
    }
  });

  ws.on('close', () => {
    console.log('客户端已断开连接');
  });
});

// 启动服务器时运行此文件，服务器会自动监听端口并响应心跳