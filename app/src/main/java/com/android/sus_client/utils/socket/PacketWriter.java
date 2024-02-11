package com.android.sus_client.utils.socket;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/* Write the message before the message is send */
public class PacketWriter {

    private DataOutputStream dos;

    public PacketWriter(Socket socket) throws IOException {
        this.dos = new DataOutputStream(socket.getOutputStream());
    }

    public void close() {
        try {
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeUTF(String content) throws IOException {
        dos.writeInt(content.getBytes(StandardCharsets.UTF_8).length);
        dos.writeUTF(content);
        dos.flush();
    }

    public void writeByte(byte[] bytes) throws IOException {
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }

}