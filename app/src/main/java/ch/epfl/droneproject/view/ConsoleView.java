package ch.epfl.droneproject.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.Stack;

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
 * The onDraw is supposed to be called on the correct thread. It is why we add buffer for other thread
 * which invalidate the view
 * Be careful on the thread you are before pushing a message on it !
 */
public class ConsoleView extends android.support.v7.widget.AppCompatEditText implements ConsoleMessages.Listener {

    public static final int MAX_NUMBER_LINE = 70;
    private int currentNumberLine = 0;

    private ArrayList<String> buffer = new ArrayList<>();

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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        while(buffer.size() > 0){
            String actualConsole = this.getText().toString();

            if (this.currentNumberLine == MAX_NUMBER_LINE) {
                int end = actualConsole.lastIndexOf('\n');
                actualConsole = actualConsole.substring(0, end);
                this.currentNumberLine--;
            }
            actualConsole = buffer.remove(0) + "\n" + actualConsole;
            this.setText(actualConsole);
            this.currentNumberLine++;
        }
    }

    @Override
    public void newMessageAdded(String message) {
        buffer.add(message);
    // this.invalidate();
    }
}