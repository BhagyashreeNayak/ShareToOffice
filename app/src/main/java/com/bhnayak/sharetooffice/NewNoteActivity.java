package com.bhnayak.sharetooffice;

import android.Manifest;
import android.app.Activity;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.stream.JsonWriter;
import com.microsoft.graph.authentication.IAuthenticationAdapter;
import com.microsoft.graph.authentication.MSAAuthAndroidAdapter;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.core.DefaultClientConfig;
import com.microsoft.graph.core.IClientConfig;
import com.microsoft.graph.extensions.DriveItem;
import com.microsoft.graph.extensions.GraphServiceClient;
import com.microsoft.graph.extensions.IGraphServiceClient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewNoteActivity extends AppCompatActivity {
    private ImageView sharedImageView;
    private TextView sharedTextView;
    private EditText titleEditText, labelEditText;
    private Button saveButton;

    //MS Graph API Integration
    private static final String LOG_TAG = "ShareToOffice";
    private static final String CLIENT_SECRET = "ivTdYQ1NrwTs0dHUFp9pqs5";
    final static String CLIENT_ID = "70d7f126-38d4-45c8-bb15-814b90be4a26";
    final static String SCOPES[] = {"https://graph.microsoft.com/User.Read"};
    final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    private IAuthenticationAdapter mAuthenticationAdapter;
    private IGraphServiceClient mClient;
    private IClientConfig mConfig;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_new_note);

        sharedImageView = (ImageView) findViewById(R.id.shared_imageview);
        sharedTextView = (TextView) findViewById(R.id.shared_textview);
        titleEditText = (EditText) findViewById(R.id.title_editText);
        labelEditText = (EditText) findViewById(R.id.label_editText);
        saveButton = (Button) findViewById(R.id.save_button);

        sharedTextView.setMovementMethod(new ScrollingMovementMethod());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //
        configureAndAcquireTokenForOD();

        // Call shared intent
        onSharedIntent();
    }

    private void configureAndAcquireTokenForOD() {
        mAuthenticationAdapter = new MSAAuthAndroidAdapter(this.getApplication()) {
            @Override
            public String getClientId() {
                return CLIENT_ID;
            }

            @Override
            public String[] getScopes() {
                return new String[]{
                        // An example set of scopes your application could use
                        "https://graph.microsoft.com/Directory.Read.All",
                        "https://graph.microsoft.com/Directory.ReadWrite.All",
                        "https://graph.microsoft.com/Files.ReadWrite.All",
                        "https://graph.microsoft.com/Files.Read.All",
                        "https://graph.microsoft.com/Sites.Read.All",
                        "https://graph.microsoft.com/Sites.ReadWrite.All",
                        "https://graph.microsoft.com/User.Read.All",
                        "https://graph.microsoft.com/User.ReadWrite.All",
                        "offline_access",
                        "openid"
                };
            }
        };
        mAuthenticationAdapter.login(this, new ICallback<Void>() {
            @Override
            public void success(final Void aVoid) {
                //Handle successful login
            }

            @Override
            public void failure(final ClientException ex) {
                //Handle failed login
            }
        });
        mConfig = DefaultClientConfig.createWithAuthenticationProvider(mAuthenticationAdapter);

        // Create the service client from the configuration
        mClient = new GraphServiceClient.Builder().fromConfig(mConfig).buildClient();
    }

    // Shared intent method
    private void onSharedIntent() {

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action)) {

            if ("text/html".equals(type) || "text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // Update UI to reflect text being shared
                    sharedTextView.setText(sharedText);
                    sharedTextView.setVisibility(View.VISIBLE);
                }
            } else if (type.startsWith("image/")) {
                sharedTextView.setVisibility(View.GONE);
                Uri receiveUri = (Uri) intent
                        .getParcelableExtra(Intent.EXTRA_STREAM);

                if (receiveUri != null) {
                    try {
                        Bitmap bitmap = Utils.decodeUri(NewNoteActivity.this,
                                receiveUri, 200);
                        sharedImageView.setImageBitmap(bitmap);
                        sharedImageView.setVisibility(View.VISIBLE);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // Handle other intents, such as being started from the home screen
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(NewNoteActivity.this, "Saving the contents to OneDrive", Toast.LENGTH_SHORT).show();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                uploadFileToOneDrive();
            }
        });
    }

    private void uploadFileToOneDrive() {
        verifyStoragePermissions( this );
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ClipDescription clipBoarddescription = NewNoteActivity.this.getIntent().getClipData().getDescription();
                    if (clipBoarddescription.hasMimeType("image/jpeg")) {
                        handleImageFileUpload();
                    } else if (clipBoarddescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || clipBoarddescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                        handleTextFileUpload();
                    }

                } catch (Exception e) {
                    Toast.makeText(NewNoteActivity.this, "Exception while Saving the contents to OneDrive", Toast.LENGTH_SHORT).show();
                }
            }
        });
        t.start();

    }

    private void handleTextFileUpload() {
        String sharedTextData = getIntent().getClipData().getItemAt(0).getText().toString();
        Date today = new Date();
        String title = titleEditText.getText().toString();
        //uploadFileToOneDrive(fileNameToUpload.trim()+".json",byteArray);
        createAndUploadJsonFile("","text",sharedTextData);
    }

    private void handleImageFileUpload() {
        Uri fileUri = getIntent().getClipData().getItemAt(0).getUri();

        // Create URI from real path
        String path;
        path = getPathFromUri(fileUri);
        fileUri = Uri.fromFile(new java.io.File(path));

        // File's binary content
        final java.io.File fileContent = new java.io.File(fileUri.getPath());
        try {
            final FileInputStream fis = new FileInputStream(fileContent);
            final String fileNameOfUploadedFile = getDateString() + path.substring(path.lastIndexOf('/') + 1);

            try {
                byte[] contentByteArray = new byte[(int) fileContent.length()];
                fis.read(contentByteArray);
                uploadFileToOneDrive(fileNameOfUploadedFile, contentByteArray);
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            createAndUploadJsonFile(fileNameOfUploadedFile, "image", "");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void dismiss() {
        Intent intent = new Intent();
        setResult(1,intent);
        this.finish();

    }

    private void createAndUploadJsonFile(String filename, String type, String textdata) {
        {
            /*{
                "device":"Android",
                "type":"image",
                "tagName":"main",
                "description":"An image from India",
                "targetFile":"image1.png",
                "textData":"",
                "timeStamp":"1/1/1",
                "srcUrl":"http://bing",
                "title":"Title"
            }*/

            String tag = labelEditText.getText().toString();
            String title = titleEditText.getText().toString();

            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent("   ");
            try {
                jsonWriter.beginObject();
                jsonWriter.name("device").value("Android");
                jsonWriter.name("type").value(type);
                jsonWriter.name("tagName").value(tag);
                jsonWriter.name("description").value("Image shared to office");
                if( filename != null && !filename.isEmpty() )
                    jsonWriter.name("targetFile").value(filename);
                jsonWriter.name("textData").value(textdata);
                jsonWriter.name("timeStamp").value(getTimeStamp());
                jsonWriter.name("srcUrl").value("");
                jsonWriter.name("title").value(title);
                jsonWriter.endObject();
                jsonWriter.close();

                final String message = stringWriter.getBuffer().toString();

                String jsonFileName;
                String datePrefix = getDateString();
                if( filename != null && !filename.isEmpty() )
                {
                    jsonFileName = datePrefix+filename.substring(0, filename.lastIndexOf('.')) + ".json";
                }
                else
                {
                    jsonFileName=datePrefix+".json";
                }
                byte[] byteArray = null;

                try {
                    byteArray = message.getBytes("UTF-8");
                    uploadFileToOneDrive(jsonFileName,byteArray);
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }


            } catch (Exception exc) {
                Log.i(LOG_TAG, "Exception thrown in json creation");
            }
        }
    }

    private void uploadFileToOneDrive(final String fileNametoUpload, final byte[] byteArrayToUpload) {
        mClient.getMe().getDrive().getRoot().getChildren("ShareToOffice").buildRequest().get(new ICallback<DriveItem>() {
            @Override
            public void success(DriveItem driveItem) {
                String folderId = driveItem.id;
                byte[] byteArray = null;

                try {
                    byteArray = fileNametoUpload.getBytes("UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    ex.printStackTrace();
                }
                mClient.getMe().getDrive().getItems().byId(folderId).getChildren(fileNametoUpload).getContent().buildRequest().put(byteArray, new ICallback<DriveItem>() {
                    @Override
                    public void success(DriveItem driveItem) {
                        mClient.getMe().getDrive().getItems().byId(driveItem.id).getContent().buildRequest().put(byteArrayToUpload, new ICallback<DriveItem>() {
                            @Override
                            public void success(DriveItem driveItem) {
                                Toast.makeText(NewNoteActivity.this, "Successfully Saved the contents to OneDrive", Toast.LENGTH_SHORT).show();
                                dismiss();
                            }

                            @Override
                            public void failure(ClientException ex) {
                                Toast.makeText(NewNoteActivity.this, "Failure while Saving the contents to OneDrive", Toast.LENGTH_SHORT).show();
                                Log.i(LOG_TAG, Log.getStackTraceString(ex));
                            }
                        });

                    }

                    @Override
                    public void failure(ClientException ex) {
                        Log.i(LOG_TAG, Log.getStackTraceString(ex));
                    }
                });

            }

            @Override
            public void failure(ClientException ex) {

            }
        });
    }

    private String getDateString()
    {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String getTimeStamp()
    {
        return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
    }
}
