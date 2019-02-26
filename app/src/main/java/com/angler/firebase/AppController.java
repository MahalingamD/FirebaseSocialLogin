package com.angler.firebase;

import android.app.Application;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

public class AppController extends Application {

   @Override
   public void onCreate() {
      super.onCreate();

      FacebookSdk.sdkInitialize(this);

   }
}
