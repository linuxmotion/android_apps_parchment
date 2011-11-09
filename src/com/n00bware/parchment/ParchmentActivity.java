
package com.n00bware.parchment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

/* 
 * TODO move .close() statements to finally blocks
 * nomenclature object prefixes are changing; to claify
 *p ParchmentActivity objects; Content from layout/main.xml
 *s Content from layout/save.xml
 *o Content from layout/open.xml
 */

public class ParchmentActivity extends Activity {

    private final String TAG = "Parchment";
    private final String FILENAME = "filename";
    private final String DIRECTORY = "directory";
    private final String DEFAULT_OPEN_PATH = "/sdcard/parchment/parchment_test";
    private final String BLANK = "";
    private final String SAVE_MARKER = "saved_text";
    private final String LAST_INDEX = "last_index";
    private String pFilename = new String();
    private String textContainer;
    private String pDir;
    private String pContents;

    private final int SAVE = 1;
    private final int SAVE_AS = 2;
    private final int OPEN = 3;

    private Dialog openDialog;
    private Dialog saveDialog;
    private File pFile;
    private File root = Environment.getExternalStorageDirectory();
    private SharedPreferences pSharedPrefs;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        File parchment_path = new File(DEFAULT_OPEN_PATH);
        if (!parchment_path.isDirectory()) {
            parchment_path.getParentFile().mkdir();
        }
        if (parchment_path == null) {
            Log.d(TAG, "shit nulled return from parchment_path");
        } else {
            Log.d(TAG, "parchment_path returned " + parchment_path.getParentFile().getName());
        }

        Bundle args = getIntent().getExtras();
        if (args != null) {
            pFilename = args.getString(FILENAME);
            setTitle(pFilename);
            Log.d(TAG, "args: " + pFilename);
        } else {
            setTitle(R.string.default_title);
            Log.d(TAG, "args: null");
        }


        if (!root.canWrite()) {Log.d(TAG, "sdcard is not writable");} else {Log.d(TAG, "sdcard is writable");}

        pSharedPrefs = getPreferences(MODE_PRIVATE);
        //TODO: best to start @ /# or sdcard/parchment/#
        pDir = pSharedPrefs.getString(DIRECTORY, "/");
        /*
         * TODO: read only is important but we should look into
         * allowing su save abilities
         */
        isReadOnly();
    }

    protected void onPause() {
        super.onPause();

        if (pFilename.equals(BLANK)) {
            EditText container = (EditText)findViewById(R.id.pDoc);
            String text = container.getText().toString();
            if (text != null) {
                SharedPreferences.Editor prefEditor = pSharedPrefs.edit();
                prefEditor.putString(SAVE_MARKER, text);
                prefEditor.commit();
            }
        }
    }

    protected void onResume() {
        super.onResume();

        if (pFilename.equals(BLANK)) {
            pContents = pSharedPrefs.getString(SAVE_MARKER, BLANK);
            EditText contents = (EditText)findViewById(R.id.pDoc);
            contents.setText(pContents);
        }
    }

    private void isReadOnly() {
        Log.d(TAG, "isReadOnly");
        pFile = new File(pFilename);

        if (pFile.canWrite()) {Log.d(TAG, "canWrite() true");}
        if (pFile.exists()) {Log.d(TAG, "exists() true");}
        if (pFile.canRead()) {Log.d(TAG, "canRead() true");}
        String file_path = pFile.getAbsolutePath();
        Log.d(TAG, String.format("file path { %s }", file_path));
        Log.d(TAG, pFile.toString());

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            Log.d(TAG, "sdcard mounted");
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
            Log.d(TAG, "sdcard readonly");
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
            Log.d(TAG, "epic failure");
        }

        loadText();
    }

    private void loadText() {
        try {
            Log.d(TAG, "loadText");
            byte[] buffer = new byte[(int)pFile.length()];
            FileInputStream reader = new FileInputStream(pFile);
            reader.read(buffer);

            textContainer = new String(buffer);
            EditText eText = (EditText)findViewById(R.id.pDoc);
            eText.setText(textContainer);
            setTitle(pFilename);
            Log.d(TAG, eText.getText().toString());
            reader.close();
        } catch(IOException e) {
            Log.d(TAG, "Parsing error while loading " + pFilename);
        }
    }

    private void writeFile() {
        try {
            EditText eText = (EditText)findViewById(R.id.pDoc);
            String text = eText.getText().toString();
            FileWriter fWrite = new FileWriter(pFile);
            Log.d(TAG, String.format("text we are attempting to write { %s }", text));
            try {
                fWrite.write(text);
                fWrite.flush();
                fWrite.close();
            } catch (IOException IOe) {
                Log.d(TAG, "IOException while FileWriter was writing file");
                IOe.printStackTrace();
            }

            String dir = pFilename.substring(0, pFilename.lastIndexOf("/"));
            Log.d(TAG, String.format("LAST_INDEX { %s } dir { %s }", LAST_INDEX, dir));
            SharedPreferences.Editor sEdit = pSharedPrefs.edit();
            sEdit.putString(LAST_INDEX, dir);
            sEdit.commit();
        } catch(IOException e) {
            Log.d(TAG, "Failed to write " + pFilename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void open() {
        openDialog = new Dialog(this);
        openDialog.setContentView(R.layout.open);
        openDialog.setTitle("Open file... { full path }");
        final EditText open_filename = (EditText) openDialog.findViewById(R.id.oNewFile);
        open_filename.setSingleLine();

        if (pFilename != null) {
            open_filename.setText(pFilename);
        } else {
            open_filename.setText(DEFAULT_OPEN_PATH);
        }

        final Button oButton = (Button) openDialog.findViewById(R.id.oButton);
        oButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                pFilename = open_filename.getText().toString();
                Log.d(TAG, pFilename);
                isReadOnly();
                openDialog.dismiss();
            }
        });
        openDialog.show();
    }

    private void savePrompt() {
        saveDialog = new Dialog(this);
        saveDialog.setContentView(R.layout.save);
        saveDialog.setTitle("Save as... { full path }");
        EditText filename = (EditText) saveDialog.findViewById(R.id.sNewFile);
        filename.setSingleLine();
        filename.setText(pDir);

        final Button sButton = (Button) saveDialog.findViewById(R.id.sButton);
        sButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                saveAs();
            }
        });

        saveDialog.show();
    }

    private void saveAs() {
        EditText fname = (EditText) saveDialog.findViewById(R.id.sNewFile);
        String filename = fname.getText().toString();
        File newFile = new File(Environment.getExternalStorageDirectory() + "/" + filename);
        String debug_filename = newFile.getAbsolutePath();
        if (!newFile.exists()) {Log.d(TAG, "saveAs newFile !exists");}
        if (!newFile.canWrite()) {Log.d(TAG, "saveAs newFile canWrite=false");}
        if (!newFile.canRead()) {Log.d(TAG, "saveAs newFile canRead=false");}
        Log.d(TAG, "the file we are attempting to save is " + debug_filename);

/*        if (!newFile.canWrite()) {
            Toast.makeText(this, "We do not have write permission for " + filename, Toast.LENGTH_LONG).show();
            saveDialog.dismiss();
            return;
        }*/

        pFile = newFile;
        Log.d(TAG, pFile.getAbsolutePath());

        if (!newFile.isFile()) {
            new AlertDialog.Builder(this)
            .setTitle(R.string.save_alert_title)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whatButton) {
                    saveDialog.dismiss();
                    writeFile();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whatButton){
                    saveDialog.dismiss();
                }
            })
            .show();
        } else {
            Log.d(TAG, "if not a file already");
            saveDialog.dismiss();
            writeFile();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean pick = super.onCreateOptionsMenu(menu);
        menu.add(0, SAVE, 0, "Save").setIcon(R.drawable.save);
        menu.add(0, SAVE_AS, 0, "Save as").setIcon(R.drawable.save_as);
        menu.add(0, OPEN, 0, "Open").setIcon(R.drawable.open);
        return pick;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SAVE:
                writeFile();
                break;
            case SAVE_AS:
                savePrompt();
                break;
            case OPEN:
                open();
        }
    return false;
    }
}
