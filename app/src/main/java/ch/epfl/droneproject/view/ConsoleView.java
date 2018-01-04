package ch.epfl.droneproject.view;

import android.content.Context;
import android.util.AttributeSet;

import ch.epfl.droneproject.ConsoleMessages;


/**
 * ConsoleView.java
 * @author blchatel
 *
 * Because we want sometimes some feedback from the computation. This small view can display texts
 * like a log console would.
 *
 * It is working like a stack where new message are displayed on top and older messages are droped
 * The stack can contains MAX_NUMBER_LINE line of message
 *
 * Be careful on the thread you are before pushing a message on it !
 */
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