package com.example.nwswitch;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;

// custom text view using asset fonts, e.g., Font Awesome
public class MyTextView extends androidx.appcompat.widget.AppCompatTextView {

    public MyTextView(Context context) {
        super(context);
        createView();
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView();

    }

    private void createView() {
        setGravity(Gravity.CENTER);
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(),
                "fonts/fontAwesome6Solid900.otf");
        setTypeface(tf);
    }
}
