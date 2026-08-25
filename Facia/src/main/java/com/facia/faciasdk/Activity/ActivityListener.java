package com.facia.faciasdk.Activity;

public interface ActivityListener {
    void terminateSdk(String event);

    void initLightSensor();

    void unregisterSensors();
}