package com.ucoachu.capacitor;

import android.content.Intent;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.ucoachu.capacitor.activities.CameraActivity;

@CapacitorPlugin(name = "Ucoachu")
public class UcoachuPlugin extends Plugin {

    private Ucoachu implementation = new Ucoachu();

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));

        Intent myIntent;
        if (value == "landscape") {
            myIntent = new Intent(getActivity(), CameraActivity.class);
            myIntent.putExtra("mode", "landscape");
        } else {
            myIntent = new Intent(getContext(), CameraActivity.class);
            myIntent.putExtra("mode", "portrait");
        }
        getActivity().startActivity(myIntent);
        call.resolve(ret);
    }
}
