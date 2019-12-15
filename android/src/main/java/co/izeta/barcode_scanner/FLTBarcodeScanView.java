package co.izeta.barcode_scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.LayoutInflater;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.platform.PlatformView;

import static io.flutter.plugin.common.MethodChannel.MethodCallHandler;

public class FLTBarcodeScanView implements PlatformView, MethodCallHandler {

    Context context;
    Registrar registrar;
    MethodChannel channel;
    BarcodeScanView view;

    FLTBarcodeScanView(Context context, Registrar registrar, int id) {
        this.context = context;
        this.registrar = registrar;
        getViewFromFile(registrar);

        channel = new MethodChannel(registrar.messenger(), "plugins.flutter.io/barcode_scanner_" + id);
        channel.setMethodCallHandler(this);
    }

    private void getViewFromFile(Registrar registrar) {
        view = new BarcodeScanView(context, registrar.activity());
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        switch (call.method) {
            case "setupCamera":
                view.setup(result);
                break;
            case "resume": {
                System.out.println("on resumed");
                view.start();
            }
                break;
            case "stop": {
                System.out.println("on Stop");
                view.stop();
            }
                break;
            default:
                result.notImplemented();
        }
    }
}