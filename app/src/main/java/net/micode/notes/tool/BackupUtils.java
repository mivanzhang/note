/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.tool;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class BackupUtils {
    private static final String TAG = "BackupUtils";
    // Singleton stuff
    private static BackupUtils sInstance;
    private static Context mContext;

    public static synchronized BackupUtils getInstance(Context context) {
        mContext = context;
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    /**
     * Following states are signs to represents backup or restore
     * status
     */
    // Currently, the sdcard is not mounted
    public static final int STATE_SD_CARD_UNMOUONTED = 0;
    // The backup file not exist
    public static final int STATE_BACKUP_FILE_NOT_EXIST = 1;
    // The data is not well formated, may be changed by other programs
    public static final int STATE_DATA_DESTROIED = 2;
    // Some run-time exception which causes restore or backup fails
    public static final int STATE_SYSTEM_ERROR = 3;
    // Backup or restore success
    public static final int STATE_SUCCESS = 4;
    public static final String DEFAULT = "小米便签";
    private TextExport mTextExport;

    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public int exportToText() {
        return mTextExport.exportToText();
    }

    public int exportToXMl() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(Environment.getExternalStorageDirectory().getAbsolutePath());
//        sb.append(mContext.getString(R.string.file_path));
//        sb.append(mContext.getString(
//                R.string.format_date_ymd,
//                DateFormat.format(mContext.getString(R.string.format_date_ymd),
//                        System.currentTimeMillis())));
//        sb.append(".xml");
//        File file = new File(sb.toString());
//        try {
//            if (!file.exists()) {
//                file.getParentFile().mkdirs();
//                file.createNewFile();
//            }
//            FileOutputStream fileos = new FileOutputStream(file);
//            XmlSerializer serializer = Xml.newSerializer();
//            // we set the FileOutputStream as output for the serializer,
//            // using UTF-8 encoding
//            serializer.setOutput(fileos, "UTF-8");
//            // <?xml version=”1.0″ encoding=”UTF-8″>
//            // Write <?xml declaration with encoding (if encoding not
//            // null) and standalone flag (if stan dalone not null)
//            // This method can only be called just after setOutput.
//            serializer.startDocument("UTF-8", null);
//            // start a tag called "root"
//            serializer.startTag(null, "root");
//            serializer.startTag(null, "folder");
//            serializer.text("root");
//            serializer.endTag(null, "folder");
////            serializer.endTag(null, "root");
//            serializer.flush();
//            fileos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return 1;
        return mTextExport.exportXML();
    }

    public String getExportedTextFileName() {
        return mTextExport.mFileName;
    }

    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory;
    }

    private static class TextExport {
        public static final String[] NOTE_PROJECTION = {
                NoteColumns.ID,
                NoteColumns.CREATED_DATE,
                NoteColumns.SNIPPET,
                NoteColumns.TYPE,
                NoteColumns.MODIFIED_DATE
        };

        public static final int NOTE_COLUMN_ID = 0;

        public static final int NOTE_COLUMN_MODIFIED_DATE = 1;

        public static final int NOTE_COLUMN_SNIPPET = 2;

        public static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT,
                DataColumns.MIME_TYPE,
                DataColumns.DATA1,
                DataColumns.DATA2,
                DataColumns.DATA3,
                DataColumns.DATA4,
        };

        public static final int DATA_COLUMN_CONTENT = 0;

        public static final int DATA_COLUMN_MIME_TYPE = 1;

        public static final int DATA_COLUMN_CALL_DATE = 2;

        public static final int DATA_COLUMN_PHONE_NUMBER = 4;

        public final String[] TEXT_FORMAT;
        public static final int FORMAT_FOLDER_NAME = 0;
        public static final int FORMAT_NOTE_DATE = 1;
        public static final int FORMAT_NOTE_CONTENT = 2;

        public static final String ROOT = "ROOT";
        public static final String FOLDER = "FOLDER";
        public static final String FOLDER_NAME = "folder_name";
        public static final String NOTE = "NOTE";
        public static final String TIME = "TIME";
        public static final String PHONENUMBER = "PHONENUMBER";
        public static final String CALL_DATE = "CALL_DATE";
        public static final String LOCALTION = "LOCALTION";
        public static final String CONTENT = "CONTENT";
        public static final String DEFAULT = "小米便签";
        private Context mContext;
        private String mFileName;
        private String mFileDirectory;

        public TextExport(Context context) {
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
            mFileName = "";
            mFileDirectory = "";
        }

        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        /**
         * Export the folder identified by folder id to text
         */
        private void exportFolderToText(String folderId, PrintStream ps) {
            // Query notes belong to this folder
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[]{
                            folderId
                    }, null);

            if (notesCursor != null) {
                if (notesCursor.moveToFirst()) {
                    do {
                        // Print note's last modified date
//                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
//                                mContext.getString(R.string.format_datetime_mdhm),
//                                notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // Query data belong to this note
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (notesCursor.moveToNext());
                }
                notesCursor.close();
            }
        }

        /**
         * Export note identified by id to a print stream
         */
        private void exportNoteToText(String noteId, PrintStream ps) {
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[]{
                            noteId
                    }, null);

            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    do {
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                        if (DataConstants.CALL_NOTE.equals(mimeType)) {
                            // Print phone number

                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(phoneNumber)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        phoneNumber));
                            }
                            // Print call date
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                    .format(mContext.getString(R.string.format_datetime_mdhm),
                                            callDate)));
                            // Print call attachment location
                            if (!TextUtils.isEmpty(location)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content));
                            }
                        }
                    } while (dataCursor.moveToNext());
                }
                dataCursor.close();
            }
            // print a line separator between note
            try {

                ps.write("\r\n\r\n\r\n".getBytes());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }


        /**
         * Note will be exported as text which is user readable
         */
        public int exportToText() {
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }

            PrintStream ps = getExportToTextPrintStream();
            if (ps == null) {
                Log.e(TAG, "get print stream error");
                return STATE_SYSTEM_ERROR;
            }
            // First export folder and its notes
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

            if (folderCursor != null) {
                if (folderCursor.moveToFirst()) {
                    do {
                        // Print folder's name
                        String folderName = "";
                        if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                            folderName = mContext.getString(R.string.call_record_folder_name);
                        } else {
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                        }
                        if (!TextUtils.isEmpty(folderName)) {
                            if (!folderName.equals(mContext.getString(R.string.call_record_folder_name))) {
                                ps.println("----------------");
                                ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                                ps.println("----------------");
                            }
                        }
                        String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                        exportFolderToText(folderId, ps);
                    } while (folderCursor.moveToNext());
                }
                folderCursor.close();
            }

            // Export notes in root's folder
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);
            ps.println("----------------");
            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), DEFAULT));
            ps.println("----------------");
            if (noteCursor != null) {
                if (noteCursor.moveToFirst()) {
                    do {
//                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
//                                mContext.getString(R.string.format_datetime_mdhm),
//                                noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // Query data belong to this note
                        String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (noteCursor.moveToNext());
                }
                noteCursor.close();
            }
            ps.close();

            return STATE_SUCCESS;
        }

        /**
         * Get a print stream pointed to the file {@generateExportedTextFile}
         */
        private PrintStream getExportToTextPrintStream() {
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format);
            if (file == null) {
                Log.e(TAG, "create file to exported failed");
                return null;
            }
            mFileName = file.getName();
            mFileDirectory = mContext.getString(R.string.file_path);
            PrintStream ps = null;
            try {
                ps = new PrintStream(file, "UTF-8");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return ps;
        }

        public int exportXML() {
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_xml_format);
            try {
                FileOutputStream fileos = new FileOutputStream(file);
                XmlSerializer serializer = Xml.newSerializer();
                // we set the FileOutputStream as output for the serializer,
                // using UTF-8 encoding
                serializer.setOutput(fileos, "UTF-8");
                // <?xml version=”1.0″ encoding=”UTF-8″>
                // Write <?xml declaration with encoding (if encoding not
                // null) and standalone flag (if stan dalone not null)
                // This method can only be called just after setOutput.
                serializer.startDocument("UTF-8", null);
                // start a tag called "root"
                serializer.startTag(null, ROOT);
                // First export folder and its notes
                Cursor folderCursor = mContext.getContentResolver().query(
                        Notes.CONTENT_NOTE_URI,
                        NOTE_PROJECTION,
                        "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                                + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                                + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

                if (folderCursor != null) {
                    if (folderCursor.moveToFirst()) {
                        do {
                            // Print folder's name
                            String folderName = "";
                            if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                                folderName = mContext.getString(R.string.call_record_folder_name);
                            } else {
                                folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                            }
                            serializer.startTag(null, FOLDER);
                            String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                            serializer.startTag(null, NoteColumns.ID);
                            serializer.text(String.valueOf(folderId));
                            serializer.endTag(null, NoteColumns.ID);
                            if (!TextUtils.isEmpty(folderName)) {
                                serializer.startTag(null, FOLDER_NAME);
                                serializer.text(folderName);
                                serializer.endTag(null, FOLDER_NAME);
                            }
                            exportFolderToXML(folderId, serializer);
                            serializer.endTag(null, FOLDER);
                        } while (folderCursor.moveToNext());
                    }
                    folderCursor.close();
                }

                // Export notes in root's folder
                Cursor noteCursor = mContext.getContentResolver().query(
                        Notes.CONTENT_NOTE_URI,
                        NOTE_PROJECTION,
                        NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                                + "=0", null, null);
                serializer.startTag(null, FOLDER);
                serializer.startTag(null, FOLDER_NAME);
                serializer.text(DEFAULT);
                serializer.endTag(null, FOLDER_NAME);
                serializer.startTag(null, NoteColumns.ID);
                serializer.text(String.valueOf(0));
                serializer.endTag(null, NoteColumns.ID);
                if (noteCursor != null) {
                    if (noteCursor.moveToFirst()) {
                        do {
                            String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                            serializer.startTag(null, NOTE);
                            serializer.startTag(null, TIME);
                            serializer.text(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                    mContext.getString(R.string.format_datetime_mdhm),
                                    noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                            serializer.endTag(null, TIME);
                            exportNoteToXML(noteId, serializer);
                            serializer.endTag(null, NOTE);
                        } while (noteCursor.moveToNext());
                    }
                    noteCursor.close();
                }
                serializer.endTag(null, FOLDER);
                serializer.endTag(null, ROOT);
                serializer.flush();
                //finally we close the file stream
                fileos.close();
            } catch (Exception e) {
                e.printStackTrace();
                return STATE_SYSTEM_ERROR;
            }
            return STATE_SUCCESS;
        }

        private void exportFolderToXML(String folderId, XmlSerializer serializer) {
            // Query notes belong to this folder
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[]{
                            folderId
                    }, null);
            try {
                if (notesCursor != null) {
                    if (notesCursor.moveToFirst()) {
                        do {
                            // Print note's last modified date

                            serializer.startTag(null, NOTE);
                            serializer.startTag(null, TIME);
                            serializer.text(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                    mContext.getString(R.string.format_datetime_mdhm),
                                    notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                            serializer.endTag(null, TIME);
                            // Query data belong to this note
                            String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                            exportNoteToXML(noteId, serializer);
                            serializer.endTag(null, NOTE);
                        } while (notesCursor.moveToNext());
                    }
                    notesCursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Export note identified by id to a print stream
         */
        private void exportNoteToXML(String noteId, XmlSerializer serializer) {
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[]{
                            noteId
                    }, null);

            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    try {
                        do {
                            String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                            if (DataConstants.CALL_NOTE.equals(mimeType)) {
                                // Print phone number
                                String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                                long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                                String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                                if (!TextUtils.isEmpty(phoneNumber)) {
                                    serializer.startTag(null, PHONENUMBER);
                                    serializer.text(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                            phoneNumber));
                                    serializer.endTag(null, PHONENUMBER);
                                }
                                // Print call date
                                serializer.startTag(null, CALL_DATE);
                                serializer.text(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                        .format(mContext.getString(R.string.format_datetime_mdhm),
                                                callDate)));
                                serializer.endTag(null, CALL_DATE);
                                // Print call attachment location
                                if (!TextUtils.isEmpty(location)) {
                                    serializer.startTag(null, LOCALTION);
                                    serializer.text(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                            location));
                                    serializer.endTag(null, LOCALTION);
                                }
                            } else if (DataConstants.NOTE.equals(mimeType)) {
                                String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                                if (!TextUtils.isEmpty(content)) {
                                    serializer.startTag(null, CONTENT);
                                    serializer.text(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                            content));
                                    serializer.endTag(null, CONTENT);
                                }
                            }
                        } while (dataCursor.moveToNext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                dataCursor.close();
            }
        }
    }

    /**
     * Generate the text file to store imported data
     */
    private static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        StringBuilder sb = new StringBuilder();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            sb.append(Environment.getExternalStorageDirectory().getAbsolutePath());
            sb.append(context.getString(filePathResId));
            sb.append(context.getString(
                    fileNameFormatResId,
                    DateFormat.format(context.getString(R.string.format_date_ymd),
                            System.currentTimeMillis())));
            File file = new File(sb.toString());
            try {
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                return file;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {//sd card not availble
            Toast.makeText(context, context.getString(R.string.sd_not_availble), Toast.LENGTH_LONG).show();
            Intent share = new Intent(Intent.ACTION_SEND);
            share.putExtra(Intent.EXTRA_STREAM,
                    Uri.fromFile(new File(sb.toString())));
            share.setType("*/*");//此处可发送多种文件
            context.startActivity(Intent.createChooser(share, context.getString(R.string.share_notes)));
        }
        return null;
    }


}


