import 'dart:async';
import 'package:flutter/foundation.dart';

import 'package:flutter/widgets.dart';
import 'package:flutter/services.dart';

class BarcodeScannerController {

  MethodChannel _channel;

  BarcodeScannerController.init(int id) {
    _channel =  new MethodChannel('plugins.flutter.io/barcode_scanner_$id');
  }

  Future<void> setupCamera() async {
    return _channel.invokeMethod('setupCamera');
  }

  Future<void> stopCamera() async {
    return _channel.invokeMethod('stop');
  }

  Future<void> startCamera() async {
    return _channel.invokeMethod('resume');
  }
}

typedef void BarcodeScannerCreatedCallback(BarcodeScannerController controller);

class NativeBarcodeScanner extends StatefulWidget {

  final BarcodeScannerCreatedCallback onBarcodeScannerCreated;

  NativeBarcodeScanner({
    Key key,
     @required this.onBarcodeScannerCreated,
  });

  @override
  _NativeBarcodeScanner createState() => _NativeBarcodeScanner();
}

class _NativeBarcodeScanner extends State<NativeBarcodeScanner> {
  @override
  Widget build(BuildContext context) {
    if(defaultTargetPlatform == TargetPlatform.android) {
        return AndroidView(
          viewType: 'barcodescanner',
          onPlatformViewCreated: onPlatformViewCreated,
          creationParamsCodec: const StandardMessageCodec(),
        );
    } else if(defaultTargetPlatform == TargetPlatform.iOS) {
        return UiKitView(
          viewType: 'barcodescanner',
          onPlatformViewCreated: onPlatformViewCreated,
          creationParamsCodec: const StandardMessageCodec(),
        );
    }

    return Text('$defaultTargetPlatform is not yet supported by this plugin');
  }

  Future<void> onPlatformViewCreated(id) async {
    if (widget.onBarcodeScannerCreated == null) {
    return;
    }
    widget.onBarcodeScannerCreated(BarcodeScannerController.init(id));
  }
}