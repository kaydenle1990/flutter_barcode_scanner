package co.izeta.barcode_scanner;

import android.content.Context;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class BarcodeScannerFactory extends PlatformViewFactory {

    private final Registrar mPluginRegistrar;

    public BarcodeScannerFactory(Registrar registrar) {
        super(StandardMessageCodec.INSTANCE);
        mPluginRegistrar = registrar;
    }

    @Override
    public PlatformView create(Context context, int i, Object o) {
        return new FLTBarcodeScanView(context, mPluginRegistrar, i);
    }
}