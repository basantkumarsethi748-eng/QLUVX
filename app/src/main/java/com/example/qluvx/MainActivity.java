
package com.example.qluvx;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(8, 8, 14));

        TextView title = new TextView(this);
        title.setText("QLUVX");
        title.setTextColor(Color.rgb(255, 55, 180));
        title.setTextSize(36);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("CONNECT • CHAT • LIVE");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 16, 0, 0);

        root.addView(title);
        root.addView(subtitle);

        setContentView(root);
    }
}
