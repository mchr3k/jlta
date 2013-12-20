package org.jlta.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * //TODO
 *
 * @author: akabelytskyi
 * @since: 12.1
 */
public class ServerCommunication {

    public enum State
    {
        DISCONNECTED,
        CONNECTED;
    }
    private State state = State.DISCONNECTED;

    private Socket socket = null;
    private ObjectInputStream dataIn = null;
    private ObjectOutputStream dataOut = null;

    private TrackingData data = new TrackingData();

    public void connect(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            dataOut = new ObjectOutputStream(socket.getOutputStream());
            dataIn = new ObjectInputStream(socket.getInputStream());
            state = State.CONNECTED;
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    public void disconnect() {
        socket = null;
        dataIn = null;
        dataOut = null;
        state = State.DISCONNECTED;
    }

    public void fetch() throws IOException, ClassNotFoundException {
        try {
            dataOut.writeObject("fetch");
            dataOut.flush();

            data = (TrackingData)dataIn.readObject();
        } catch (IOException e) {
            disconnect();
            throw e;
        }catch (ClassNotFoundException e) {
            disconnect();
            throw e;
        }
    }

    public void reset() throws IOException {
        try {
            dataOut.writeObject("reset");
            dataOut.flush();
        } catch (IOException e) {
            disconnect();
            throw e;
        }
    }

    public TrackingData getData() {
        return data;
    }

    public State getState() {
        return state;
    }
}
