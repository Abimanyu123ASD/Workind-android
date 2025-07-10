package cbt.com;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cbt.com.CommonFunctions;
import cbt.com.MainActivity;
import cbt.com.R;

public class ErrorActivity extends CommonFunctions {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
        TextView ERR_TEXTVIEW = (TextView)findViewById(R.id.textView);
        ERR_TEXTVIEW.setText(ERR_DESC);
        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isNetworkAvailable())
                    openMainActivity();
            }
        });
    }
    public void openMainActivity() {
        Intent intent2 = new Intent(this, MainActivity.class);
        startActivity(intent2);
        finish();
    }

}