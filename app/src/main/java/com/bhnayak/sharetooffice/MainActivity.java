package com.bhnayak.sharetooffice;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.MsalServiceException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.User;
import com.microsoft.onedrivesdk.saver.ISaver;
import com.microsoft.onedrivesdk.saver.Saver;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private FloatingActionButton mAddNewNoteButton;
    private ISaver mSaver;

    private static final String TAG = "ShareToOffice";
    private static final String LOG_TAG = "ShareToOffice";
    private static final String CLIENT_SECRET = "ivTdYQ1NrwTs0dHUFp9pqs5";
    final static String CLIENT_ID = "70d7f126-38d4-45c8-bb15-814b90be4a26";
    final static String SCOPES[] = {"https://graph.microsoft.com/User.Read"};
    final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    private IAuthenticationAdapter mAuthenticationAdapter;
    private IGraphServiceClient mClient;
    private IClientConfig mConfig;

    /* Azure AD Variables */
    private PublicClientApplication sampleApp;
    private AuthenticationResult authResult;


    private void setUp() {
        mAddNewNoteButton = (FloatingActionButton) findViewById(R.id.fab);
        mAddNewNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return onNavigationItemSelected(item);
            }
        });
    }

    private void uploadFileToOneDrive() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ClipDescription clipBoarddescription = MainActivity.this.getIntent().getClipData().getDescription();
                    if (clipBoarddescription.hasMimeType("image/jpeg")) {
                        handleImageFileUpload();
                    } else if (clipBoarddescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || clipBoarddescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                        handleTextFileUpload();
                    }

                } catch (Exception e) {
                    Snackbar.make(MainActivity.this.findViewById(R.id.drawer_layout), "Onedrive upload failed", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
        });
        t.start();

    }

    private void handleTextFileUpload() {

    }

    private void handleImageFileUpload() {
        Uri fileUri = getIntent().getClipData().getItemAt(0).getUri();

        // Create URI from real path
        String path;
        path = getPathFromUri(fileUri);
        fileUri = Uri.fromFile(new java.io.File(path));

        ContentResolver cR = MainActivity.this.getContentResolver();

        // File's binary content
        final java.io.File fileContent = new java.io.File(fileUri.getPath());
        //  View v = null;
        final Activity activity = MainActivity.this;
        //mSaver.startSaving(activity, path, Uri.parse(fileUri.toString()));
        //createAndUploadJsonFile( path.substring( path.lastIndexOf('/') + 1), "image", "", "dummy" );
        try {
            final FileInputStream fis = new FileInputStream(fileContent);
            final String fileNameOfUploadedFile = path.substring( path.lastIndexOf('/') + 1);
            byte[] byteArray = null;

            try {
                byteArray = fileNameOfUploadedFile.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }

            mClient.getMe().getDrive().getRoot().getChildren("ShareToOffice").buildRequest().get(new ICallback<DriveItem>() {
                @Override
                public void success(DriveItem driveItem) {
                    String folderId = driveItem.id;
                    byte[] byteArray = null;

                    try {
                        byteArray = fileNameOfUploadedFile.getBytes("UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                    }
                    mClient.getMe().getDrive().getItems().byId(folderId).getChildren(fileNameOfUploadedFile).getContent().buildRequest().put(byteArray, new ICallback<DriveItem>() {
                        @Override
                        public void success(DriveItem driveItem) {
                            // This is the new content that we use to update the file
                            byte[] byteArray = new byte[(int) fileContent.length()];

                            try {
                                fis.read(byteArray);

                                mClient.getMe().getDrive().getItems().byId(driveItem.id).getContent().buildRequest().put(byteArray, new ICallback<DriveItem>() {
                                    @Override
                                    public void success(DriveItem driveItem) {
                                        //Snackbar.make(MainActivity.this.findViewById(R.id.drawer_layout), driveItem.getRawObject().getAsString(), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                    }

                                    @Override
                                    public void failure(ClientException ex) {
                                        Log.i(LOG_TAG, Log.getStackTraceString(ex));
                                    }
                                });
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
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
            /*mClient.getMe().getDrive().getRoot().getChildren(fileNameOfUploadedFile).getContent().buildRequest().put(byteArray, new ICallback<DriveItem>() {
                @Override
                public void success(DriveItem driveItem) {
                    // This is the new content that we use to update the file
                    byte[] byteArray = new byte[(int) fileContent.length()];

                    try {
                        fis.read(byteArray);

                        mClient.getMe().getDrive().getItems().byId(driveItem.id).getContent().buildRequest().put(byteArray, new ICallback<DriveItem>() {
                            @Override
                            public void success(DriveItem driveItem) {
                                //Snackbar.make(MainActivity.this.findViewById(R.id.drawer_layout), driveItem.getRawObject().getAsString(), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                            }

                            @Override
                            public void failure(ClientException ex) {
                                Log.i(LOG_TAG, Log.getStackTraceString(ex));
                            }
                        });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void failure(ClientException ex) {
                    Log.i(LOG_TAG, Log.getStackTraceString(ex));
                }
            });
*/        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void createAndUploadJsonFile(String filename, String type, String textdata, String tag) {
        {
            /*device:"Android",
              type:"image",
              tagName:"hack1",
              description:"An image from India 3",
              targetFile:"image3.png",
              textData:""
              timeStamp:"1/1/1",
              srcUrl:"http://bing",*/

            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            jsonWriter.setIndent("   ");
            try {
                jsonWriter.beginObject();
                jsonWriter.name("device").value("Android");
                jsonWriter.name("type").value(type);
                jsonWriter.name("tag").value(tag);
                jsonWriter.name("description").value("Image shared to office");
                jsonWriter.name("targetFile").value(filename);
                jsonWriter.name("textData").value(textdata);
                jsonWriter.name("timeStamp").value((new Date()).getTime());
                jsonWriter.name("srcUrl").value("");
                jsonWriter.endObject();
                jsonWriter.close();

                String message = stringWriter.getBuffer().toString();
                File outdir = getCacheDir();
                File jsonFile = File.createTempFile(filename.substring(0, filename.lastIndexOf('.')), ".json", outdir);
                FileWriter fileWriter = new FileWriter(jsonFile);
                fileWriter.write(message);
                fileWriter.close();
                mSaver.startSaving(MainActivity.this, jsonFile.getPath(), Uri.parse(Uri.fromFile(jsonFile).toString()));

            } catch (Exception exc) {
                Log.i(LOG_TAG, "Exception thrown in json creation");
            }
        }
    }

    private void uploadFileToOneDrive2() {
        String guid = UUID.randomUUID().toString();
        byte[] byteArray = null;

        try {
            byteArray = guid.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }

        mClient
                .getMe()
                .getDrive()
                .getRoot()
                .getChildren(guid)
                .getContent()
                .buildRequest()
                .put(byteArray, new ICallback<DriveItem>() {
                    @Override
                    public void success(DriveItem driveItem) {
                        // This is the new content that we use to update the file
                        byte[] byteArray = null;

                        try {
                            byteArray = "A plain text file".getBytes("UTF-8");

                            mClient
                                    .getMe()
                                    .getDrive()
                                    .getItems()
                                    .byId(driveItem.id)
                                    .getContent()
                                    .buildRequest()
                                    .put(byteArray, new ICallback<DriveItem>() {
                                        @Override
                                        public void success(DriveItem driveItem) {
                                            //Snackbar.make(MainActivity.this.findViewById(R.id.drawer_layout), driveItem.getRawObject().getAsString(), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                        }

                                        @Override
                                        public void failure(ClientException ex) {
                                            Log.i(LOG_TAG, Log.getStackTraceString(ex));
                                        }
                                    });
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void failure(ClientException ex) {
                        Log.i(LOG_TAG, Log.getStackTraceString(ex));
                    }
                });
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureAndAcquireTokenForOD();

        mSaver = Saver.createSaver(CLIENT_ID);


        String[] scope = {"onedrive.readwrite", "offline_access", "onedrive.appfolder"};
        String redirectURL = "http://localhost:8080/";

        //CLIENT_SECRET = "qpbC4SGKmb2ei5KSkKn0F9n";

// auto login
        //mClient = new Client(CLIENT_ID, scope, redirectURL, CLIENT_SECRET);
// self login

        /*mClient.login();
        try {
            FolderItem root = mClient.getRootDir();
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        }*/

        setUp();

        String action = getIntent().getAction();
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            uploadFileToOneDrive();
        }
    }

    private void configureAndAcquireTokenForOD() {
        mAuthenticationAdapter = new MSAAuthAndroidAdapter(this.getApplication()) {
            @Override
            public String getClientId() {
                return MainActivity.CLIENT_ID;
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
        /* Configure your sample app and save state for this activity */
        sampleApp = null;
        if (sampleApp == null) {
            sampleApp = new PublicClientApplication(this.getApplicationContext(), CLIENT_ID);
        }

  /* Attempt to get a user and acquireTokenSilent
   * If this fails we do an interactive request
   */
        List<User> users = null;

        try {
            users = sampleApp.getUsers();

            if (users != null && users.size() == 1) {
          /* We have 1 user */

                sampleApp.acquireTokenSilentAsync(SCOPES, users.get(0), getAuthSilentCallback());
            } else {
          /* We have no user */

          /* Let's do an interactive request */
                sampleApp.acquireToken(this, SCOPES, getAuthInteractiveCallback());
            }
        } catch (MsalClientException e) {
            Log.d(TAG, "MSAL Exception Generated while getting users: " + e.toString());

        } catch (IndexOutOfBoundsException e) {
            Log.d(TAG, "User at this position does not exist: " + e.toString());
        }

        //sampleApp.acquireToken(getActivity(), SCOPES, getAuthInteractiveCallback());
        // Use the authentication provider previously defined within the project and create a configuration instance

        mAuthenticationAdapter.login(getActivity(), new ICallback<Void>() {
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

    //
// App callbacks for MSAL
// ======================
// getActivity() - returns activity so we can acquireToken within a callback
// getAuthSilentCallback() - callback defined to handle acquireTokenSilent() case
// getAuthInteractiveCallback() - callback defined to handle acquireToken() case
//

    public Activity getActivity() {
        return this;
    }

    /* Callback method for acquireTokenSilent calls
     * Looks if tokens are in the cache (refreshes if necessary and if we don't forceRefresh)
     * else errors that we need to do an interactive request.
     */
    private AuthenticationCallback getAuthSilentCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
            /* Successfully got a token, call Graph now */
                Log.d(TAG, "Successfully authenticated");

            /* Store the authResult */
                authResult = authenticationResult;

            /* call graph */
                callGraphAPI();

            /* update the UI to post call Graph state */
                //updateSuccessUI();
            }

            @Override
            public void onError(MsalException exception) {
            /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                /* Exception when communicating with the STS, likely config issue */
                } else if (exception instanceof MsalUiRequiredException) {
                /* Tokens expired or no session, retry with interactive */
                }
            }

            @Override
            public void onCancel() {
            /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }


    /* Callback used for interactive request.  If succeeds we use the access
         * token to call the Microsoft Graph. Does not check cache
         */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
            /* Successfully got a token, call graph now */
                Log.d(TAG, "Successfully authenticated");
                Log.d(TAG, "ID Token: " + authenticationResult.getIdToken());

            /* Store the auth result */
                authResult = authenticationResult;

            /* call Graph */
                callGraphAPI();

            /* update the UI to post call Graph state */
                //updateSuccessUI();
            }

            @Override
            public void onError(MsalException exception) {
            /* Failed to acquireToken */
                Log.d(TAG, "Authentication failed: " + exception.toString());

                if (exception instanceof MsalClientException) {
                /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                /* Exception when communicating with the STS, likely config issue */
                }
            }

            @Override
            public void onCancel() {
            /* User canceled the authentication */
                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    /* Handles the redirect from the System Browser */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    /* Use Volley to make an HTTP request to the /me endpoint from MS Graph using an access token */
    private void callGraphAPI() {
        Log.d(TAG, "Starting volley request to graph");

    /* Make sure we have a token to send to graph */
        if (authResult.getAccessToken() == null) {
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject parameters = new JSONObject();

        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d(TAG, "Failed to put parameters: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, MSGRAPH_URL,
                parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
            /* Successfully called graph, process data and send to UI */
                Log.d(TAG, "Response: " + response.toString());

                //updateGraphUI(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authResult.getAccessToken());
                return headers;
            }
        };

        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());

        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
