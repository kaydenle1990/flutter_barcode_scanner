import 'package:flutter/material.dart';
import 'package:barcode_scanner/barcode_scanner.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WidgetsBindingObserver {
  
  BarcodeScannerController barcodeController;

  @override
  void initState() {
    WidgetsBinding.instance.addObserver(this);
    super.initState();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    if (state == AppLifecycleState.resumed) {
      this.barcodeController.startCamera();
    } else if (state == AppLifecycleState.inactive) {
      this.barcodeController.stopCamera();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {

    NativeBarcodeScanner barcodeScanner = NativeBarcodeScanner(onBarcodeScannerCreated: onBarcodeScannerCreated,);

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Container(
          width: double.infinity,
          height: double.infinity,
          child: SizedBox(
            width: 200.0,
            height: 200.0,
            child: barcodeScanner,
          ),
        ),
      ),
    );
  }

  void onBarcodeScannerCreated(BarcodeScannerController controller) {
    this.barcodeController = controller;
    this.barcodeController.setupCamera();
  }
}
