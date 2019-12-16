package co.izeta.barcode_scanner;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.fragment.app.Fragment;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.Telephone;
import ezvcard.property.Url;

import io.flutter.plugin.common.MethodChannel;

public class BarcodeScanView extends RelativeLayout {

    private static final int NO_TYPE = -1;
    private static final String[] PHONE_TYPE_STRINGS = {"home", "work", "mobile", "fax", "pager", "main"};

    private static final int[] PHONE_TYPE_VALUES = {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN,
    };

    public static final String[] PHONE_TYPE_KEYS = {
            ContactsContract.Intents.Insert.PHONE_TYPE,
            ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
            ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE
    };

    public static final String[] PHONE_KEYS = {
            ContactsContract.Intents.Insert.PHONE,
            ContactsContract.Intents.Insert.SECONDARY_PHONE,
            ContactsContract.Intents.Insert.TERTIARY_PHONE
    };

    private static final String[] EMAIL_TYPE_STRINGS = {"home", "work", "mobile"};
    private static final int[] EMAIL_TYPE_VALUES = {
            ContactsContract.CommonDataKinds.Email.TYPE_HOME,
            ContactsContract.CommonDataKinds.Email.TYPE_WORK,
            ContactsContract.CommonDataKinds.Email.TYPE_MOBILE,
    };

    public static final String[] EMAIL_KEYS = {
            ContactsContract.Intents.Insert.EMAIL,
            ContactsContract.Intents.Insert.SECONDARY_EMAIL,
            ContactsContract.Intents.Insert.TERTIARY_EMAIL
    };

    private static final String[] ADDRESS_TYPE_STRINGS = {"home", "work"};
    private static final int[] ADDRESS_TYPE_VALUES = {
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
            ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK,
    };

    private ScannerOverlay overlay;

    private View overlayView;
    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    Activity mActivity;
    private boolean deAttached = false;
    private MethodChannel.Result result;

    public BarcodeScanView(Context context) {
        super(context);
    }

    public BarcodeScanView(Context context, Activity activity) {
        super(context);
        this.mActivity = activity;
    }

    public BarcodeScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarcodeScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BarcodeScanView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (surfaceView == null) {
            initComponents();
            initialiseDetectorsAndSources();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cameraSource.release();
    }

    public void setup(MethodChannel.Result result) {
        this.result = result;
    }

    public void start() {
        deAttached = false;
    }

    public void stop() {
        deAttached = true;
    }

    private void initComponents() {
        surfaceView = new SurfaceView(getContext());
        overlayView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_overlay, null);

        this.addView(surfaceView);
        this.addView(overlayView);
    }

    private void initialiseDetectorsAndSources() {
        Toast.makeText(getContext(), "Barcode scanner started", Toast.LENGTH_SHORT).show();
        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera();
            }
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barCode = detections.getDetectedItems();
                if (barCode.size() > 0) {
                    setBarCode(barCode);
                }
            }
        });
    }

    private void openCamera(){
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraSource.start(surfaceView.getHolder());
            } else {
                ActivityCompat.requestPermissions(mActivity, new
                        String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setBarCode(final SparseArray<Barcode> barCode){
        if (!deAttached) {
            String intentData = barCode.valueAt(0).rawValue;
            deAttached = true;
            System.out.println("==> Raw data: " + intentData + "\n");
            // Get V-Card information
            if (intentData.contains("VCARD")) {
                VCard vcard = Ezvcard.parse(intentData).first();
                
                // open vcard
                showContactActivity(vcard);

                // success
                if (this.result != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result.success(null);
                        }
                    });
                }
            } else {
                if (this.result != null) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            result.success(null);
                        }
                    });
                }
            }
        }
    }

    // get information to show in contact activity
    private static int toEmailContractType(String typeString) {
        return doToContractType(typeString, EMAIL_TYPE_STRINGS, EMAIL_TYPE_VALUES);
    }

    private static int toPhoneContractType(String typeString) {
        return doToContractType(typeString, PHONE_TYPE_STRINGS, PHONE_TYPE_VALUES);
    }

    private static int toAddressContractType(String typeString) {
        return doToContractType(typeString, ADDRESS_TYPE_STRINGS, ADDRESS_TYPE_VALUES);
    }

    private static int doToContractType(String typeString, String[] types, int[] values) {
        if (typeString == null) {
            return NO_TYPE;
        }
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            if (typeString.startsWith(type) || typeString.startsWith(type.toUpperCase(Locale.ENGLISH))) {
                return values[i];
            }
        }
        return NO_TYPE;
    }

    private void showContactActivity(VCard vcard) {

        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);
        ArrayList<ContentValues> data = new ArrayList<>();

        // Name
        String fullName = vcard.getFormattedName().getValue();
        intent.putExtra(ContactsContract.Intents.Insert.NAME, fullName);

        // Company
        if (vcard.getOrganization().getValues() != null) {
            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, vcard.getOrganization().getValues().get(0));
        }

        // Job title
        if (vcard.getTitles() != null) {
            intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, vcard.getTitles().get(0).getValue());
        }

        // Address
        List<Address> address = vcard.getAddresses();
        if (address != null) {
            for (Address info : address) {
                for (AddressType type : info.getTypes()) {
                    int typeText = toAddressContractType(type.toString());
                    intent.putExtra(ContactsContract.Intents.Insert.POSTAL, info.getStreetAddressFull())
                            .putExtra(ContactsContract.Intents.Insert.POSTAL_TYPE, typeText);
                }
            }
        }

        // Nick name
        if (vcard.getNickname() != null) {
            List<String> nicknames = vcard.getNickname().getValues();
            if (nicknames != null) {
                for (String nickname : nicknames) {
                    if (nickname != null && !nickname.isEmpty()) {
                        ContentValues row = new ContentValues(3);
                        row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
                        row.put(ContactsContract.CommonDataKinds.Nickname.TYPE,
                                ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT);
                        row.put(ContactsContract.CommonDataKinds.Nickname.NAME, nickname);
                        data.add(row);
                        break;
                    }
                }
            }
        }

        // URLs
        List<Url> urls = vcard.getUrls();
        if (urls != null) {
            for (Url url : urls) {
                if (url != null && !url.getValue().isEmpty()) {
                    ContentValues row = new ContentValues(2);
                    row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
                    row.put(ContactsContract.CommonDataKinds.Website.URL, url.getValue());
                    data.add(row);
                    break;
                }
            }
        }

        // Get birthday
        Birthday birthday = vcard.getBirthday();
        if (birthday != null) {
            ContentValues row = new ContentValues(3);
            row.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
            row.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
            row.put(ContactsContract.CommonDataKinds.Event.START_DATE, birthday.getText());
            data.add(row);
        }

        if (!data.isEmpty()) {
            intent.putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, data);
        }

        // Get phone number
        List<Telephone> phoneNumbers = vcard.getTelephoneNumbers();
        if (phoneNumbers != null) {
            for (int x = 0; x < Math.min(phoneNumbers.size(), PHONE_KEYS.length); x++) {
                String phoneNumer = phoneNumbers.get(x).getText();
                List<TelephoneType> telephoneTypes = phoneNumbers.get(x).getTypes();
                for (TelephoneType type : telephoneTypes) {
                    int rawType = toPhoneContractType(type.getValue());
                    intent.putExtra(PHONE_KEYS[x], phoneNumer)
                            .putExtra(PHONE_TYPE_KEYS[x], rawType);
                }
            }
        }

        // Get email number
        List<Email> emails = vcard.getEmails();
        if (emails != null) {
            for (int x = 0; x < emails.size(); x++) {
                String email = emails.get(x).getValue();
                List<EmailType> emailTypes = emails.get(x).getTypes();
                for (EmailType type : emailTypes) {
                    int rawType = toEmailContractType(type.getValue());
                    intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                            .putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, rawType);
                }
            }
        }

        mActivity.startActivity(intent);

        /**
         *
         *     BEGIN:VCARD
         *     VERSION:3.0
         *     N:LE;Anh Tai
         *     FN:Anh Tai LE
         *     ORG:iZeta LLC
         *     TITLE:Technical Lead
         *     ADR:;;09 Nguyen Gia Thieu street, district 6, ward 3;Ho Chi Minh;;700000;Viet Nam
         *     TEL;CELL:+84 (0) 786780018
         *     EMAIL;WORK;INTERNET:anhtai.le@izeta.co
         *     URL:https://www.izeta.co
         *     END:VCARD
         *
         * */

        /*
        *
        * public static final String ACTION = "android.intent.action.INSERT";
            public static final String COMPANY = "company";
            public static final String DATA = "data";
            public static final String EMAIL = "email";
            public static final String EMAIL_ISPRIMARY = "email_isprimary";
            public static final String EMAIL_TYPE = "email_type";
            public static final String EXTRA_ACCOUNT = "android.provider.extra.ACCOUNT";
            public static final String EXTRA_DATA_SET = "android.provider.extra.DATA_SET";
            public static final String FULL_MODE = "full_mode";
            public static final String IM_HANDLE = "im_handle";
            public static final String IM_ISPRIMARY = "im_isprimary";
            public static final String IM_PROTOCOL = "im_protocol";
            public static final String JOB_TITLE = "job_title";
            public static final String NAME = "name";
            public static final String NOTES = "notes";
            public static final String PHONE = "phone";
            public static final String PHONETIC_NAME = "phonetic_name";
            public static final String PHONE_ISPRIMARY = "phone_isprimary";
            public static final String PHONE_TYPE = "phone_type";
            public static final String POSTAL = "postal";
            public static final String POSTAL_ISPRIMARY = "postal_isprimary";
            public static final String POSTAL_TYPE = "postal_type";
            public static final String SECONDARY_EMAIL = "secondary_email";
            public static final String SECONDARY_EMAIL_TYPE = "secondary_email_type";
            public static final String SECONDARY_PHONE = "secondary_phone";
            public static final String SECONDARY_PHONE_TYPE = "secondary_phone_type";
            public static final String TERTIARY_EMAIL = "tertiary_email";
            public static final String TERTIARY_EMAIL_TYPE = "tertiary_email_type";
            public static final String TERTIARY_PHONE = "tertiary_phone";
            public static final String TERTIARY_PHONE_TYPE = "tertiary_phone_type";
        *
        * */

    }
}
