
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

public class Camera2Module extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public Camera2Module(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "Camera2Android";
  }
}