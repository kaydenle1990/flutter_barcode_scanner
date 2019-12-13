import Flutter
import UIKit


public class SwiftBarcodeScannerPlugin: NSObject, FlutterPlugin {

  public static func register(with registrar: FlutterPluginRegistrar) {
    
    let barcodeScanFactory = FLTBarcodeScanViewFactory(registrar.messenger())
    registrar.register(barcodeScanFactory, withId: "barcodescanner")
  }
}
