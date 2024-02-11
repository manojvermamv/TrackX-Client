package com.android.sus_client.utils.socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/* Read the message size from the header and return the message of the correct size */
public class ObjectReader {

    private ObjectInputStream ois;

    public ObjectReader(Socket socket) throws IOException {
        this.ois = new ObjectInputStream(socket.getInputStream());
    }

    public void close() {
        try {
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readUTF() throws IOException {
        //int length = dis.readInt();
        StringBuilder string = new StringBuilder();
        while (ois.available() > 0) {
            string.append(ois.readUTF());
        }
        return string.toString();
    }

    public byte[] readByte() throws IOException {
        int length = ois.readInt();
        byte[] bytes = new byte[length];
        ois.readFully(bytes, 0, length);
        return bytes;
    }

    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> clazz) throws IOException, ClassNotFoundException {
        Object object = ois.readObject();
        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        }
        return (T) object;
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject() throws IOException, ClassNotFoundException {
        return (T) ois.readObject();
    }

}