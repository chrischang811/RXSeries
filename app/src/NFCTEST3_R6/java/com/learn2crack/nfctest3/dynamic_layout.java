package com.learn2crack.rx1000;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

public class dynamic_layout extends AppCompatActivity {
    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dynamic_layout);

        linearLayout = findViewById(R.id.linear_layout);
        //Adding 2 TextViews
       /* for (int i = 1; i <= 50; i++) {
            TextView textView = new TextView(this);
            textView.setText("TextView " + String.valueOf(i));
            linearLayout.addView(textView);
            addLineSeperator();
        }*/
        for (int i = 1; i <= 3; i++) {
            addTextViews(i);
            addCheckBoxes(i);
            addLineSeperator();
        }
    }

    private void addTextViews(int i) {
        //Adding a LinearLayout with HORIZONTAL orientation
        LinearLayout textLinearLayout = new LinearLayout(this);
        textLinearLayout.setOrientation(LinearLayout.VERTICAL);

        linearLayout.addView(textLinearLayout);

        //for (int i = 1; i <= 3; i++) {
            TextView textView = new TextView(this);
            textView.setText("TextView " + String.valueOf(i));
            setTextViewAttributes(textView);
            textLinearLayout.addView(textView);
        //}

    }

    private void setTextViewAttributes(TextView textView) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                0, 0
        );

        textView.setTextColor(Color.WHITE);
        textView.setLayoutParams(params);
    }


    private void addCheckBoxes(int i) {

        LinearLayout checkBoxLayout = new LinearLayout(this);
        checkBoxLayout.setOrientation(LinearLayout.VERTICAL);

        linearLayout.addView(checkBoxLayout);

      //  for (int i = 1; i <= 3; i++) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText("CheckBox " + String.valueOf(i));
            setCheckBoxAttributes(checkBox);
            checkBoxLayout.addView(checkBox);
      //  }
       // addLineSeperator();
    }


    private void setCheckBoxAttributes(CheckBox checkBox) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(convertDpToPixel(16),
                convertDpToPixel(16),
                convertDpToPixel(16),
                0
        );

        checkBox.setLayoutParams(params);

        //This is used to place the checkbox on the right side of the textview
        //By default, the checkbox is placed at the left side
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple,
                typedValue, true);

        checkBox.setButtonDrawable(null);
        checkBox.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                typedValue.resourceId, 0);
    }

    private void addLineSeperator() {
        LinearLayout lineLayout = new LinearLayout(this);
        lineLayout.setBackgroundColor(Color.GRAY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2);
        params.setMargins(0, convertDpToPixel(10), 0, convertDpToPixel(10));
        lineLayout.setLayoutParams(params);
        linearLayout.addView(lineLayout);
    }

    private int convertDpToPixel(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }
}
