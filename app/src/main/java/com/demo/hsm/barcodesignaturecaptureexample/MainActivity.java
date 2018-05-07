package com.demo.hsm.barcodesignaturecaptureexample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.aidc.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements BarcodeReader.BarcodeListener{

    private static final String TAG  = "HoneywellScannerTEST";
    private static BarcodeReader barcodeReader;
    private ImageView imageSignature;
    TextView txtBarcode;
    TextView txtGuidance;
    Bitmap[] images;
    private LruCache<String, Bitmap> mMemoryCache;

    private AidcManager manager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        txtBarcode=(TextView)findViewById(R.id.txtBarcode);
        txtGuidance=(TextView)findViewById(R.id.txtGuidance);
        imageSignature=(ImageView)findViewById(R.id.imageView);

        txtBarcode.setText("Press Scan Button");
        txtGuidance.setText("follow guidance");

        // create the AidcManager providing a Context and a
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                claimScanner();
            }
        });
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        //prepare image cache
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        loadImages();
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

    void loadImages(){
        addBitmapToMemoryCache(Signature.GUIDANCE_MOVE_LEFT, BitmapFactory.decodeResource(getResources(), R.drawable.left));
        addBitmapToMemoryCache(Signature.GUIDANCE_MOVE_RIGHT, BitmapFactory.decodeResource(getResources(), R.drawable.right));
        addBitmapToMemoryCache(Signature.GUIDANCE_MOVE_UP, BitmapFactory.decodeResource(getResources(), R.drawable.up));
        addBitmapToMemoryCache(Signature.GUIDANCE_MOVE_DOWN, BitmapFactory.decodeResource(getResources(), R.drawable.down));
        addBitmapToMemoryCache(Signature.GUIDANCE_MOVE_OUT, BitmapFactory.decodeResource(getResources(), R.drawable.farer));

        addBitmapToMemoryCache(Signature.GUIDANCE_UNSUPPORTED_SYMBOLOGY, BitmapFactory.decodeResource(getResources(), R.drawable.unsupported));
    }
    void claimScanner(){
        try {
            barcodeReader.claim();
            setParameters(barcodeReader);
            barcodeReader.addBarcodeListener(MainActivity.this);
            txtBarcode.setText("Press Scan Button");
            txtGuidance.setText("follow guidance");

            Log.d(TAG, "AidcManager.onCreated: Scanner claimed");
        }catch(Exception e){
            Log.d(TAG, "Scanner currently unavailable");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        if (barcodeReader != null) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        com.honeywell.aidc.Signature sig = barcodeReader.getSignature(getParameters());
                        String guidance=sig.getGuidance();

                        Log.d(TAG, "getGuidance=" + guidance);
                        if(guidance==Signature.GUIDANCE_SUCCESS) {
                            txtBarcode.setText(barcodeReadEvent.getBarcodeData());
                            if (sig.getImage() != null) {
                                imageSignature.setImageBitmap(null);
                                imageSignature.setImageBitmap(sig.getImage());
                            } else {
                                Log.d(TAG, "getImage=null !");
                            }
                            Log.d(TAG, "onBarcodeEvent: setImageBitmap");
                            txtGuidance.setText("Ready for next");
                        }
                        else{
                            imageSignature.setImageBitmap(getBitmapFromMemCache(guidance));
                            txtGuidance.setText(guidance);
imageSignature.invalidate();
                        }
                    } catch (ScannerUnavailableException ex) {
                        Log.d(TAG, ex.getMessage());
                        Toast.makeText(getApplicationContext(), "GetSig failed: " + ex.getMessage(), Toast.LENGTH_LONG);
                    } catch (ScannerNotClaimedException sncex) {
                        Log.d(TAG, sncex.getMessage());
                        Toast.makeText(getApplicationContext(), "GetSig: Scanner not claimed", Toast.LENGTH_LONG);
                    }
                }
            });
        }
    }
    @Override
    public void onFailureEvent(BarcodeFailureEvent barcodeFailureEvent) {

    }
    private SignatureParameters getParameters() {
        SignatureParameters parameters = new SignatureParameters();

        parameters.setBinarized(true);
        parameters.setResolution(4);
        parameters.setAspectRatio(0);
        parameters.setHorizontalOffset(0);
        parameters.setVerticalOffset(88);
        parameters.setWidth(440);
        parameters.setHeight(136);

        return parameters;
    }
    void setParameters(BarcodeReader barcodeReader){
        Map<String, Object> properties = new HashMap<String, Object>();
        // Set Symbologies On/Off
        properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
        properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, false);
        properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, false);
        properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, false);
        properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, false);
        properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, false);

        properties.put(BarcodeReader.PROPERTY_IATA_25_ENABLED, true);
        properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);

        // Set Max Code 39 barcode length
        properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
        // Turn on center decoding
        properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
        // Disable bad read response, handle in onFailureEvent
        properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, false);
        // Apply the settings
        barcodeReader.setProperties(properties);
        Log.d(TAG, "setParameters: done");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (barcodeReader != null) {
            // close BarcodeReader to clean up resources.
            barcodeReader.close();
            barcodeReader = null;
        }

        if (manager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            manager.close();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        claimScanner();
    }

}
