import asyncio
import websockets
import pyaudio
import whisper
import numpy as np
import wave
import time
import os
import json

# 加载 Whisper 模型
print("Loading whisper model...")
model = whisper.load_model("medium")
print("Whisper model initialized...")

# 定义音频流的参数
CHUNK = 1024  # 每次读取的数据块大小
FORMAT = pyaudio.paInt16  # 音频格式
CHANNELS = 1  # 声道数量
RATE = 16000  # 采样率
SAVE_DIRECTORY = "E:/audioFile"  # 保存音频的目录

# 创建保存音频的目录
os.makedirs(SAVE_DIRECTORY, exist_ok=True)

async def transcribe_audio(websocket, path):
    # 创建 PyAudio 对象
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT,
                    channels=CHANNELS,
                    rate=RATE,
                    input=True,
                    frames_per_buffer=CHUNK)

    print("Recording and transcribing in real-time...")

    frames = []  # 用于存储实时音频帧
    for i in range(0, int(RATE / CHUNK)):  # 计算要读多少次，每秒的采样率/每次读多少数据*录音时间=需要读多少次
        data = stream.read(CHUNK)  # 每次读chunk个数据
        frames.append(data)  # 将读出的数据保存到列表中
    print("* done recording")  # 结束录音标志

    stream.stop_stream()  # 停止输入流
    stream.close()  # 关闭输入流
    p.terminate()  # 终止pyaudio

    # 保存音频文件
    timestamp = int(time.time())  # 获取当前时间戳
    audio_filename = os.path.join(SAVE_DIRECTORY, f"{timestamp}.wav")

    wf = wave.open(audio_filename, 'wb')  # 以’wb‘二进制流写的方式打开一个文件
    wf.setnchannels(CHANNELS)  # 设置音轨数
    wf.setsampwidth(p.get_sample_size(FORMAT))  # 设置采样点数据的格式，和FOMART保持一致
    wf.setframerate(RATE)  # 设置采样率与RATE要一致
    wf.writeframes(b''.join(frames))  # 将声音数据写入文件

    print(f"Audio saved as {audio_filename}")
    frames = []  # 清空已处理的帧

    # 使用 Whisper 进行转录
    result = model.transcribe(audio_filename, language='ja', fp16=True)
    transcription = result["text"].strip()

    # 将转录结果发送到客户端
    await websocket.send(json.dumps({"type": "ok", "transcription": transcription}))

if __name__ == "__main__":
    try:
        start_server = websockets.serve(transcribe_audio, "localhost", 3000)
        asyncio.get_event_loop().run_until_complete(start_server)
        print("WebSocket server started at ws://localhost:3000/ws")
        asyncio.get_event_loop().run_forever()
    except Exception as e:
        print(f"Failed to start WebSocket server: {e}")