package com.cjapps.playinsync;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateProfileActivity extends Activity {

    //TODO Clean up code, remove phone verification, complete photo change and upload

    private ImageButton edit_name;
    private TextView name;
    private Button submit_phone_number;
    private EditText phone_number;
    private ImageView profile_pic;

    private Intent main_activity;
    private Activity create_profile_activity;

    private DatabaseManager db_manager;

    public ImageLoader profile_pic_loader;

    private AlertDialog.Builder dialog_builder ;
    private Dialog edit_name_dialog;

    private String phone_number_from_sim;

    private String[] profile_details;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_GALLERY = 2;
    private static final int CROP_FROM_CAMERA = 3;

    private Uri mImageCaptureUri ;

    private TelephonyManager tele_manager;

    private String exotel_number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        db_manager = new DatabaseManager(this);

        profile_details = db_manager.getMeTableDetails(new String[]{DatabaseManager.ME_NAME
                ,DatabaseManager.ME_DP_PATH});

        profile_pic_loader = new ImageLoader(this);
        ImageLoader.MODE = 0;

        // FOR DIALOG
        final EditText name_edittext = new EditText(this);

        tele_manager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        phone_number_from_sim = tele_manager.getLine1Number();

        tele_manager.listen(new VerifyPhoneNumber(),PhoneStateListener.LISTEN_CALL_STATE);


        dialog_builder = new AlertDialog.Builder(this);
        dialog_builder.setTitle("Enter your name");
        dialog_builder.setIcon(R.drawable.ic_action_edit);
        dialog_builder.setView(name_edittext);

        dialog_builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = name_edittext.getText().toString();
                if(value.length()<2){
                    Toast.makeText(getApplicationContext(),"Invalid Name",Toast.LENGTH_SHORT)
                            .show();
                    //edit_name_dialog.show();
                }else {
                    db_manager.updateMyDetails("", value, "", "", "", "", "", "", "", "", "", "");
                    name.setText(value);

                    // UPDATE NAME ONLINE
                    String U_ID = db_manager.getMeTableDetails(new String[]{DatabaseManager.ME_U_ID})[0];
                    new UpdateNameTask().execute(U_ID, value);
                }
            }
        });

        dialog_builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        edit_name_dialog = dialog_builder.create();

        create_profile_activity = this;
        main_activity = new Intent(this,MainActivity.class);

        profile_pic = (ImageView)findViewById(R.id.profile_picture_imagview);
        name = (TextView)findViewById(R.id.name_textview);
        phone_number = (EditText)findViewById(R.id.phone_number_edittext);
        submit_phone_number = (Button)findViewById(R.id.submit_phone_number_button);
        edit_name = (ImageButton)findViewById(R.id.edit_name_imagebutton);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        ViewGroup.LayoutParams params = profile_pic.getLayoutParams();
        params.width = size.x-32 ;
        params.height = size.x-32 ;
        profile_pic.setLayoutParams(params);

        if(phone_number_from_sim!=null) {

            phone_number.setVisibility(View.GONE);
            submit_phone_number.setVisibility(View.GONE);

            ImageView phone_separator = (ImageView) findViewById(R.id.phone_number_separator);
            phone_separator.setVisibility(View.GONE);

            TextView phone_label = (TextView)findViewById(R.id.phone_number_label);
            phone_label.setVisibility(View.GONE);

            TextView phone_info = (TextView)findViewById(R.id.phone_number_info);
            phone_info.setVisibility(View.GONE);
        }

        edit_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(edit_name_dialog.isShowing()){
                    edit_name_dialog.dismiss();
                }
                edit_name_dialog.show();
            }
        });

        submit_phone_number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String number = phone_number.getText().toString();
                if(number.length()<10){
                    Toast.makeText(getApplicationContext(),"Invalid number", Toast.LENGTH_SHORT)
                            .show();
                }else{

                    // START PHONE VERIFICATION PROCESS
                    new VerifyPhoneNumberTask().execute(number);
                }
            }
        });

        profile_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(create_profile_activity);
                builder.setTitle("Profile photo")
                        .setItems(new String[]{"Gallery","Camera","Remove"}, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which){
                                    case 0:{

                                        Intent intent = new Intent();

                                        intent.setType("image/*");
                                        intent.setAction(Intent.ACTION_GET_CONTENT);

                                        startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_GALLERY);
                                        break;
                                    }
                                    case 1:{

                                        Intent intent 	 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                                        mImageCaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),
                                                "tmp_avatar_" + String.valueOf(System.currentTimeMillis()) + ".jpg"));

                                        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mImageCaptureUri);

                                        try {
                                            intent.putExtra("return-data", true);

                                            startActivityForResult(intent, PICK_FROM_CAMERA);
                                        } catch (ActivityNotFoundException e) {
                                            e.printStackTrace();
                                        }

                                        break;
                                    }
                                    case 2:{
                                        break;
                                    }
                                }
                            }
                        });
                AlertDialog d = builder.create();

                d.show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case PICK_FROM_CAMERA: {

                doCrop();
                break;
            }
            case PICK_FROM_GALLERY: {
                mImageCaptureUri = data.getData();

                doCrop();

                break;
            }
            case CROP_FROM_CAMERA: {
                Bundle extras = data.getExtras();

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");

                    profile_pic.setImageBitmap(photo);
                }

                File f = new File(mImageCaptureUri.getPath());

                if (f.exists()) {
                    f.delete();
                }

                break;
            }
        }
    }

    private void doCrop() {
        final ArrayList<CropOption> cropOptions = new ArrayList<CropOption>();

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setType("image/*");

        List<ResolveInfo> list = getPackageManager().queryIntentActivities( intent, 0 );

        int size = list.size();

        if (size == 0) {
            Toast.makeText(this, "Can not find image crop app", Toast.LENGTH_SHORT).show();

        } else {
            intent.setData(mImageCaptureUri);

            intent.putExtra("outputX", 300);
            intent.putExtra("outputY", 300);
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("scale", true);
            intent.putExtra("return-data", true);

            if (size == 1) {
                Intent i 		= new Intent(intent);
                ResolveInfo res	= list.get(0);

                i.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                startActivityForResult(i, CROP_FROM_CAMERA);
            } else {
                for (ResolveInfo res : list) {
                    final CropOption co = new CropOption();

                    co.title 	= getPackageManager().getApplicationLabel(res.activityInfo.applicationInfo);
                    co.icon		= getPackageManager().getApplicationIcon(res.activityInfo.applicationInfo);
                    co.appIntent= new Intent(intent);

                    co.appIntent.setComponent( new ComponentName(res.activityInfo.packageName, res.activityInfo.name));

                    cropOptions.add(co);
                }

                CropOptionAdapter adapter = new CropOptionAdapter(getApplicationContext(), cropOptions);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Crop App");
                builder.setAdapter( adapter, new DialogInterface.OnClickListener() {
                    public void onClick( DialogInterface dialog, int item ) {
                        startActivityForResult( cropOptions.get(item).appIntent, CROP_FROM_CAMERA);
                    }
                });

                builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel( DialogInterface dialog ) {

                        if (mImageCaptureUri != null ) {
                            getContentResolver().delete(mImageCaptureUri, null, null );
                            mImageCaptureUri = null;
                        }
                    }
                } );

                AlertDialog alert = builder.create();

                alert.show();
            }
        }
    }
            
    @Override
    protected void onStart() {
        super.onStart();

        profile_pic_loader.DisplayImage(profile_details[1],R.drawable.ic_action_person,profile_pic);
        name.setText(profile_details[0]);
    }

    private class UpdateNameTask extends AsyncTask<String,String,Boolean>{

        @Override
        protected Boolean doInBackground(String... param) {
            try {


                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://www.playinsync.com/mobile/update_name.php");

                // Add your data
                List<NameValuePair> post_data = new ArrayList<NameValuePair>();

                post_data.add(new BasicNameValuePair("U_ID", param[0]));
                post_data.add(new BasicNameValuePair("NAME", param[1]));

                httppost.setEntity(new UrlEncodedFormEntity(post_data));

                HttpResponse response = httpclient.execute(httppost);

                if(response!=null){
                    return true;
                }

            }catch (Exception ignored){

            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            Toast.makeText(getApplicationContext(),"Name updated successfully",Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private class VerifyPhoneNumberTask extends AsyncTask<String,String,Boolean>{

        @Override
        protected Boolean doInBackground(String... param) {
            try {


                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://www.playinsync.com/mobile/make_verification_call.php");

                // Add your data
                List<NameValuePair> post_data = new ArrayList<NameValuePair>();

                post_data.add(new BasicNameValuePair("NUMBER", param[0]));

                httppost.setEntity(new UrlEncodedFormEntity(post_data));

                HttpResponse response = httpclient.execute(httppost);

                if(response!=null){
                    return true;
                }

            }catch (Exception ignored){

            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }
    }

    private class VerifyPhoneNumber extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if(incomingNumber.contains("2230770124") & state == TelephonyManager.CALL_STATE_RINGING) {

                Runtime runtime = Runtime.getRuntime();

                try {

                    runtime.exec("service call phone 5 \n");

                    exotel_number = incomingNumber;

                    Log.e("ENDED",incomingNumber+"");

                    new UploadPhoneNumberTask().execute(incomingNumber);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class UploadPhoneNumberTask extends AsyncTask<String,String,Boolean>{

        @Override
        protected Boolean doInBackground(String... param) {
            try {

                String U_ID = db_manager.getMeTableDetails(new String[]{DatabaseManager.ME_U_ID})[0];

                db_manager.updateMyDetails("","","","",param[0],"","","","","","","");

                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://www.playinsync.com/mobile/update_phone_number.php");

                // Add your data
                List<NameValuePair> post_data = new ArrayList<NameValuePair>();

                post_data.add(new BasicNameValuePair("U_ID", U_ID ));
                post_data.add(new BasicNameValuePair("NUMBER", param[0]));

                httppost.setEntity(new UrlEncodedFormEntity(post_data));

                HttpResponse response = httpclient.execute(httppost);

                if(response!=null){
                    return true;
                }

            }catch (Exception ignored){

            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            getContentResolver().delete(CallLog.Calls.CONTENT_URI
                    , CallLog.Calls.NUMBER + " = ? "
                    , new String[]{exotel_number});

            Toast.makeText(getApplicationContext(),"Phone Number saved",Toast.LENGTH_SHORT).show();

            create_profile_activity.finish();
            startActivity(main_activity);
        }
    }
}
