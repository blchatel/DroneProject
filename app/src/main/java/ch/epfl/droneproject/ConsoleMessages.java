package ch.epfl.droneproject;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class ConsoleMessages{

    private static final int MAX_MESSAGE_NUMBER = 70;
    private String[] messages;
    private int mCurrent;


    public interface Listener {
        void newMessageAdded(String message);
    }
    private final List<Listener> mListeners;

    ConsoleMessages(){
        messages = new String[MAX_MESSAGE_NUMBER];
        mCurrent = 0;
        mListeners = new ArrayList<>();
    }

    //region Listener functions
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }
    //endregion Listener


    public void pushMessage(String message){
        messages[mCurrent] = message;
        mCurrent = (mCurrent+1) % MAX_MESSAGE_NUMBER;
        notifyNewMessage(message);
    }

    private void notifyNewMessage(String newMessage) {
        for (Listener listener : mListeners) {
            listener.newMessageAdded(newMessage);
        }
    }

}
