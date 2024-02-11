package com.android.sus_client.utils.socket;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/* Write the message before the message is send */
public class ObjectWriter {

    private ObjectOutputStream oos;

    public ObjectWriter(Socket socket) throws IOException {
        this.oos = new ObjectOutputStream(socket.getOutputStream());
    }

    public void close() {
        try {
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeUTF(String content) throws IOException {
        oos.writeInt(content.getBytes(StandardCharsets.UTF_8).length);
        oos.writeUTF(content);
        oos.flush();
    }

    public void writeByte(byte[] bytes) throws IOException {
        oos.writeInt(bytes.length);
        oos.write(bytes);
        oos.flush();
    }

    public void write(Object object) throws IOException {
        oos.writeObject(object);
        oos.flush();
    }

}