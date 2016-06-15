package org.wangchenlong.contentproviderdemo;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Uri uri = Uri.parse("content://org.wangchenlong.bookprovider");
        ContentResolver resolver = getContentResolver();
        resolver.query(uri, null, null, null, null);
        resolver.query(uri, null, null, null, null);
        resolver.query(uri, null, null, null, null);
    }
}
