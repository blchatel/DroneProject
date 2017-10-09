package ch.epfl.droneproject.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by root on 09.10.17.
 */

public class ConsoleView extends android.support.v7.widget.AppCompatEditText{

    public static final String messageTest = "Test";

    public ConsoleView(Context context) {
        super(context);
    }

    public ConsoleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConsoleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    public void push(String message){

        String actualConsole = this.getText().toString();

        message = ""+this.getLineCount()+", "+this.getMaxLines();

        if(this.getLineCount() == this.getMaxLines()){
            int end = actualConsole.lastIndexOf('\n');
            actualConsole = actualConsole.substring(0, end);
        }
        this.setText(message + "\n"+ actualConsole);
    }




}
