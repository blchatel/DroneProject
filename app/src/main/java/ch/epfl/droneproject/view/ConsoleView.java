package ch.epfl.droneproject.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import ch.epfl.droneproject.ConsoleMessages;
import ch.epfl.droneproject.DroneApplication;


public class ConsoleView extends android.support.v7.widget.AppCompatEditText implements ConsoleMessages.Listener {
    public static final int MAX_NUMBER_LINE = 70;
    private int currentNumberLine = 0;

    public ConsoleView(Context context) {
        super(context);
    }

    public ConsoleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConsoleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void newMessageAdded(String message) {

        String actualConsole = this.getText().toString();

        if (this.currentNumberLine == MAX_NUMBER_LINE) {

            int end = actualConsole.lastIndexOf('\n');
            actualConsole = actualConsole.substring(0, end);
            this.currentNumberLine--;
        }

        this.setText(message + "\n" + actualConsole);
        this.currentNumberLine++;
    }
}