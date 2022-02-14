package org.moire.opensudoku.gui;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import org.moire.opensudoku.R;
import org.moire.opensudoku.db.SudokuDatabase;
import org.moire.opensudoku.game.SudokuGame;
import org.moire.opensudoku.utils.AndroidUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TitleScreenActivity extends ThemedActivity {

    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private final int MENU_ITEM_SETTINGS = 0;
    private final int MENU_ITEM_ABOUT = 1;
    private final int DIALOG_ABOUT = 0;
    private Button mResumeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title_screen);

        mResumeButton = findViewById(R.id.resume_button);
        Button mSudokuListButton = findViewById(R.id.sudoku_lists_button);
        Button mSettingsButton = findViewById(R.id.settings_button);
        Button mTakePicButton = findViewById(R.id.scan_puzzle);

        setupResumeButton();

        mSudokuListButton.setOnClickListener((view) ->
                startActivity(new Intent(this, FolderListActivity.class)));

        mSettingsButton.setOnClickListener((view) ->
                startActivity(new Intent(this, GameSettingsActivity.class)));

        mTakePicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askCameraPermissions();

            }
        });
        // check the preference to skip the title screen and launch the folder list activity
        // directly
        SharedPreferences gameSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean showSudokuFolderListOnStartup = gameSettings.getBoolean("show_sudoku_lists_on_startup", false);
        if (showSudokuFolderListOnStartup) {
            startActivity(new Intent(this, FolderListActivity.class));
        } else {
            // show changelog on first run
            Changelog changelog = new Changelog(this);
            changelog.showOnFirstRun();
        }
    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }
        else{
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            Bitmap image = (Bitmap) data.getExtras().get("data");

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CAMERA_PERM_CODE){
            if(grantResults.length < 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openCamera();
            }
            else{
                Toast.makeText(this, "camera permission required to use camera", Toast.LENGTH_SHORT).show();
            }
        }



    }
    private File createImageFile() throws IOException {
        String currentPhotoPath;
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void openCamera() {
        Toast.makeText(this, "Camera open request", Toast.LENGTH_SHORT).show();
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera, CAMERA_REQUEST_CODE);

    }

    private boolean canResume(long mSudokuGameID) {
        SudokuDatabase mDatabase = new SudokuDatabase(getApplicationContext());
        SudokuGame mSudokuGame = mDatabase.getSudoku(mSudokuGameID);
        if (mSudokuGame != null) {
            return mSudokuGame.getState() != SudokuGame.GAME_STATE_COMPLETED;
        }
        return false;
    }

    private void setupResumeButton() {
        SharedPreferences gameSettings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long mSudokuGameID = gameSettings.getLong("most_recently_played_sudoku_id", 0);
        if (canResume(mSudokuGameID)) {
            mResumeButton.setVisibility(View.VISIBLE);
            mResumeButton.setOnClickListener((view) -> {
                Intent intentToPlay = new Intent(TitleScreenActivity.this, SudokuPlayActivity.class);
                intentToPlay.putExtra(SudokuPlayActivity.EXTRA_SUDOKU_ID, mSudokuGameID);
                startActivity(intentToPlay);
            });
        } else {
            mResumeButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_SETTINGS, 0, R.string.settings)
                .setShortcut('0', 's')
                .setIcon(R.drawable.ic_settings);

        menu.add(0, MENU_ITEM_ABOUT, 1, R.string.about)
                .setShortcut('1', 'h')
                .setIcon(R.drawable.ic_info);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_SETTINGS:
                startActivity(new Intent(this, GameSettingsActivity.class));
                return true;

            case MENU_ITEM_ABOUT:
                showDialog(DIALOG_ABOUT);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        LayoutInflater factory = LayoutInflater.from(this);

        if (id == DIALOG_ABOUT) {
            final View aboutView = factory.inflate(R.layout.about, null);
            TextView versionLabel = aboutView.findViewById(R.id.version_label);
            String versionName = AndroidUtils.getAppVersionName(getApplicationContext());
            versionLabel.setText(getString(R.string.version, versionName));
            return new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.app_name)
                    .setView(aboutView)
                    .setPositiveButton("OK", null)
                    .create();
        }

        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupResumeButton();
    }
}
