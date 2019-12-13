//
//  BarcodeScanViewFactory.swift
//  Runner
//
//  Created by Anh Tai LE on 10/12/2019.
//  Copyright © 2019 The Chromium Authors. All rights reserved.
//

import Flutter
import UIKit
import AVFoundation
import Contacts
import ContactsUI

class FLTBarcodeScanViewFactory: NSObject, FlutterPlatformViewFactory {

    var messenger: FlutterBinaryMessenger!

    init(_ messenger: FlutterBinaryMessenger) {
        super.init()
        self.messenger = messenger
    }
    
    func create(withFrame frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?) -> FlutterPlatformView {
        return FLTBarcodeScanViewController(frame, viewId: viewId, args: args, messenger: messenger)
    }
    
    func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

class FLTBarcodeScanViewController: NSObject, FlutterPlatformView {
    
    var frame: CGRect!
    var viewId: Int64!
    var channel: FlutterMethodChannel!
    var scanView: BarcodeScanView!
    
    init(_ frame: CGRect, viewId: Int64, args: Any?, messenger: FlutterBinaryMessenger) {
        super.init()
        
        self.frame = frame
        self.viewId = viewId
        let channelName = "plugins.flutter.io/barcode_scanner_\(viewId)"
        self.channel = FlutterMethodChannel(name: channelName, binaryMessenger: messenger)
        self.scanView = BarcodeScanView(frame: frame)
        self.channel.setMethodCallHandler { (call: FlutterMethodCall, result: @escaping FlutterResult) in
            self.onMethodCall(call: call, result: result)
        }
    }
    
    func view() -> UIView {
        return scanView
    }
    
    func onMethodCall(call: FlutterMethodCall, result: @escaping FlutterResult) {
        let method = call.method
        switch method {
        case "setupCamera":
            scanView.requestAccessToCamera(result: result)
//            scanView.configScanView()
        case "resume":
            scanView.resume()
            result(nil);
        case "stop":
            scanView.stop()
            result(nil);
        default:
            result(FlutterMethodNotImplemented)
        }
    }
}

class BarcodeScanView: UIView {
    
    let titleAccessDenied = "Camera access denied"
    let msgAccessDenied = "Not granted access to Camera. Please enable Camera access in Settings"
    let settingsActionTitle = "Go to Settings"
    let cancelActionTitle = "Cancel"
    
    var captureSession: AVCaptureSession!
    var videoPreviewLayer: AVCaptureVideoPreviewLayer!
    var scanRect: CGRect!
    var scanOverlay: ScannerOverlay!
    var result: FlutterResult?
    
    // MARK: Setup camera
    
    /// Authorize access to Camera
    func requestAccessToCamera(result: @escaping FlutterResult) {
        self.result = result
        if AVCaptureDevice.authorizationStatus(for: .video) == .authorized {
            setupCamera(result: result)
        } else if AVCaptureDevice.authorizationStatus(for: .video) == .denied {
            DispatchQueue.main.async {
                self.openSettingsAlertView()
                result(nil)
            }
        } else {
            AVCaptureDevice.requestAccess(for: .video, completionHandler: { (granted: Bool) in
                DispatchQueue.main.sync {
                    if granted {
                        self.setupCamera(result: result)
                    } else {
                        self.openSettingsAlertView()
                        result(nil)
                    }
                }
            })
        }
    }
    
    override func willMove(toWindow newWindow: UIWindow?) {
        super.willMove(toWindow: newWindow)
        if frame.width > 0 && scanOverlay != nil {
            if newWindow == nil {
                // UIView disappear
                self.stop()
            } else {
                // UIView appear
                self.resume()
            }
        }
    }
    
    override func layoutSubviews() {
        if videoPreviewLayer != nil {
            self.videoPreviewLayer.frame = layer.bounds
            if frame.width > 0 {
                if scanOverlay == nil {
                    scanOverlay = ScannerOverlay(frame: bounds)
                    scanOverlay.backgroundColor = UIColor.clear
                    addSubview(scanOverlay)
                }
                self.scanOverlay.frame = layer.bounds
                scanRect = scanOverlay.scanRect
                configScanView()
            }
        }
    }
    
    /// Init Capture Session + Scan overlay
    func setupCamera(result: @escaping FlutterResult) {
        captureSession = AVCaptureSession()
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            result(FlutterError(code: "setupCamera_failed", message: "Failed to setup Camera", details: "Can't found Video capture device"))
            return
        }
        let videoInput: AVCaptureDeviceInput
        
        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            result(FlutterError(code: "setupCamera_failed", message: "Failed to setup Camera", details: "Can't found AV Capture device input"))
            return
        }
        
        // Video Input
        if (captureSession.canAddInput(videoInput)) {
            captureSession.addInput(videoInput)
        } else {
            showError()
            
            result(FlutterError(code: "setupCamera_failed", message: "Failed to setup Camera", details: "Can't add AV Capture device input"))
            
            return
        }
        
        // MetaOutput
        let metadataOutput = AVCaptureMetadataOutput()
        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            showError()
            
            result(FlutterError(code: "setupCamera_failed", message: "Failed to setup Camera", details: "Can't add AV Capture device input"))
            
            return
        }
        
        // Video Preview Layer
        videoPreviewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        videoPreviewLayer.frame = layer.bounds
        videoPreviewLayer.videoGravity = .resizeAspectFill
        layer.addSublayer(videoPreviewLayer)
        
        // Scanner Overlay
        backgroundColor = UIColor.purple
        
        result(nil)
    }
    
    /// Set area avaiable scanning
    @objc func didChangeCaptureInputPortFormatDescription(notification: NSNotification) {
        if let metadataOutput = captureSession.outputs.last as? AVCaptureMetadataOutput {
            let rect = videoPreviewLayer.metadataOutputRectConverted(fromLayerRect: scanRect)
            metadataOutput.rectOfInterest = rect
        }
    }
    
    /// Setting alert to open permission
    func openSettingsAlertView() {
        
        let alertController = UIAlertController(title: titleAccessDenied, message: msgAccessDenied, preferredStyle: .alert)
        
        let nextAction = UIAlertAction(title: settingsActionTitle, style: .default) { (action) in
            self.goToSettings()
        }
        alertController.addAction(nextAction)
        
        let cancelAction = UIAlertAction(title: cancelActionTitle, style: .destructive, handler: nil)
        alertController.addAction(cancelAction)
        
        UIApplication.shared.keyWindow?.rootViewController?.present(alertController, animated: true, completion: nil)
    }
    
    /// Go to settings, set permission camera access
    @objc func goToSettings() {
        guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else { return }
        if UIApplication.shared.canOpenURL(settingsUrl) {
            if #available(iOS 10.0, *) {
                UIApplication.shared.open(settingsUrl, completionHandler: { (success) in })
            } else {
                // Fallback on earlier versions
                UIApplication.shared.openURL(settingsUrl)
            }
        }
    }
    
    /// Config camera
    func configScanView() {
        // Fixed Scan boundary
        NotificationCenter.default.removeObserver(self, name: .AVCaptureInputPortFormatDescriptionDidChange, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(didChangeCaptureInputPortFormatDescription(notification:)), name: .AVCaptureInputPortFormatDescriptionDidChange, object: nil)

        // Start Camera
        if (captureSession?.isRunning == false) {
            captureSession.startRunning()
            scanOverlay.startAnimating()
        }
    }
    
    /// Dispose
    func dispose() {
        // Remove notification
        NotificationCenter.default.removeObserver(self, name: .AVCaptureInputPortFormatDescriptionDidChange, object: nil)
        
        // Stop capture session
        if (captureSession?.isRunning == true) {
            captureSession.stopRunning()
        }
    }
    
    func resume() {
        if (captureSession?.isRunning == false) {
            captureSession.startRunning()
            scanOverlay.startAnimating()
        }
    }
    
    func stop() {
        if (captureSession?.isRunning == true) {
            captureSession.stopRunning()
            if scanOverlay != nil { scanOverlay.stopAnimating() }
        }
    }
    
    
    // MARK: Handle error
    func showError() {
        let alertController = UIAlertController(title: "Scanning not supported", message: "Your device does not support scanning a code from an item. Please use a device with a camera.", preferredStyle: .alert)
        alertController.addAction(UIAlertAction(title: "OK", style: .default))
        captureSession = nil
        videoPreviewLayer = nil
        
        // Show alert
        if let rootController = UIApplication.shared.keyWindow?.rootViewController {
            rootController.present(alertController, animated: true)
        }
    }
}

// MARK: Handle AVCapture + Contact delegate
extension BarcodeScanView: AVCaptureMetadataOutputObjectsDelegate, CNContactViewControllerDelegate {
    
    fileprivate func openContactController(_ contact: CNContact) {
        let contactViewController = CNContactViewController(forNewContact: contact)
        contactViewController.contactStore = CNContactStore()
        contactViewController.delegate = self
        
        contactViewController.shouldShowLinkedContacts = true
                                                    
        contactViewController.view.layoutIfNeeded()
        let navigationController = UINavigationController(rootViewController: contactViewController)
        
        // Show Contact application
        if let rootController = UIApplication.shared.keyWindow?.rootViewController {
            rootController.present(navigationController, animated: true)
        }
    }
    
    /// Scan QRCode output handling
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        
        if metadataObjects.count > 0, let object = metadataObjects[0] as? AVMetadataMachineReadableCodeObject,
           object.type == AVMetadataObject.ObjectType.qr {
            
            guard let value = object.stringValue else { return }
            
            // Stop camera
            captureSession.stopRunning()
            
            // Make read content
            if value.contains("BEGIN:VCARD") {
                guard let data = value.data(using: .utf8) else { return }
                
                do {
                    let contacts: [CNContact] = try CNContactVCardSerialization.contacts(with: data)
                    if let contact = contacts.first {
                        DispatchQueue.main.async {
                            self.openContactController(contact)
                        }
                    }
                } catch {
                    print(error.localizedDescription)
                    // MARK: Make result error here
                    if let result = self.result {
                        result(FlutterError(code: "scanQRCode_failed", message: "Failed to scan VCard format", details: error.localizedDescription))
                    }
                }
            } else {
                // MARK: Show QRCode content
                if let result = self.result {
                    result(value) // result the qrcode value in another case of Format
                }
            }
        }
    }
    
    /// Contact Controller Handling
    func contactViewController(_ viewController: CNContactViewController, shouldPerformDefaultActionFor property: CNContactProperty) -> Bool {
        return true
    }
    
    func contactViewController(_ viewController: CNContactViewController, didCompleteWith contact: CNContact?) {
        viewController.dismiss(animated: true, completion: nil)
    }
}
