package com.android.sus_client.utils.socket;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/* Read the message size from the header and return the message of the correct size */
public class PacketReader {

    private DataInputStream dis;

    public PacketReader(Socket socket) throws IOException {
        this.dis = new DataInputStream(socket.getInputStream());
    }

    public void close() {
        try {
            dis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readUTF() throws IOException {
        //int length = dis.readInt();
        StringBuilder string = new StringBuilder();
        while (dis.available() > 0) {
            string.append(dis.readUTF());
        }
        return string.toString();
    }

    public byte[] readByte() throws IOException {
        int length = dis.readInt();
        byte[] bytes = new byte[length];
        dis.readFully(bytes, 0, length);
        return bytes;
    }

    /**
     * Extra
     */
    int pos = 0;
    byte[] buffer = new byte[16000];

    private byte[] readMessageByte() throws IOException {
        while (true) {
            int read;

            try {
                read = dis.read(buffer, pos, buffer.length - pos);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            if (read < 0) {
                break;
            }
            pos += read;
            if (pos < 4) {
                continue;
            }
            int len = dis.readInt();
            if (len < 0 || len > buffer.length) {
                break;
            }

            // not enough data
            if (pos < (len += 4)) {
                continue;
            }
            byte[] request = new byte[len];
            System.arraycopy(buffer, 4, request, 0, request.length);

            // move data of next message to the front of the buffer
            System.arraycopy(buffer, 4 + len, buffer, 0, pos - request.length);
            pos = 0;
            return request;
        }
        return null;
    }

}