package ch.epfl.droneproject.view;

import android.content.Context;
import android.util.AttributeSet;


public class ConsoleView extends android.support.v7.widget.AppCompatEditText {

    public static final int MAX_NUMBER_LINE = 70;

    private int currentNumberLine;

    public ConsoleView(Context context) {
        super(context);

        currentNumberLine = 0;
    }

    public ConsoleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConsoleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void push(String message){

        String actualConsole = this.getText().toString();

        if(this.currentNumberLine == MAX_NUMBER_LINE){
            int end = actualConsole.lastIndexOf('\n');
            actualConsole = actualConsole.substring(0, end);
            this.currentNumberLine--;
        }
        this.setText(message + "\n"+ actualConsole);
        this.currentNumberLine++;
    }




}
