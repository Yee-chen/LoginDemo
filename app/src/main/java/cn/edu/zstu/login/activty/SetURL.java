package cn.edu.zstu.login.activty;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import cn.edu.zstu.login.utils.URL;

import cn.edu.zstu.login.R;

public class SetURL extends Activity {

    private Button mSetBtn;
    private EditText mUrlEt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_url);

        mUrlEt = (EditText)findViewById(R.id.url);
        mUrlEt.setText(URL.getURL(this));
        mSetBtn = (Button)findViewById(R.id.btn);
        mSetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URL.setUrl(SetURL.this, mUrlEt.getText().toString());
                finish();
            }
        });
    }
}
