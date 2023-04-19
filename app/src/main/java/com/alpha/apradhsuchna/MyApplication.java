package com.alpha.apradhsuchna;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

import io.michaelrocks.paranoid.Obfuscate;

@Obfuscate
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}
