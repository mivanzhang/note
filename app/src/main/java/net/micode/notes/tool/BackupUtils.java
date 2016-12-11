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
import java.util.ArrayList;


public class BackupUtils {
    private static final String TAG = "BackupUtils";
    // Singleton stuff
    private static BackupUtils sInstance;
    private static Context mContext;
    public static final String ROOT = "XM_NOTES";
    public static final String FOLDER = "FOLDER";
    public static final String FOLDER_NAME = "foldername";
    public static final String NOTE = "NOTE";
    public static final String CREATE_TIME = "createTime";
    public static final String ALERT_TIME = "alertTime";
    public static final String DESKTOPX = "desktopX";
    public static final String FIX_TIME = "fixTime";
    public static final String BGCOLORID = "bgcolorId";
    public static final String FOLDERID = "folderId";
    public static final String PHONENUMBER = "PHONENUMBER";
    public static final String CALL_DATE = "CALL_DATE";
    public static final String LOCALTION = "LOCALTION";
    public static final String CONTENT = "content";
    public static final String DEFAULT = "木兮便签";
    public static final String SORT_ORDER = NoteColumns.ID+" DESC";

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
    private TextExport mTextExport;

    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public int exportToText(boolean isShare) {
        return mTextExport.exportToText(isShare);
    }

    public int exportToXMl(boolean isShare) {
        return mTextExport.exportXML(isShare);
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
                NoteColumns.MODIFIED_DATE,
                NoteColumns.PARENT_ID,
                NoteColumns.BG_COLOR_ID,
        };

        public static final int NOTE_COLUMN_ID = 0;

        public static final int NOTE_COLUMN_CREATED_DATE = 1;

        public static final int NOTE_COLUMN_SNIPPET = 2;

        public static final int NOTE_COLUMN_MODIFIED__DATE = 4;

        public static final int NOTE_PARENT_ID = 5;

        public static final int NOTE_BG_COLOR_ID = 6;


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
                    }, SORT_ORDER);

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
                    }, SORT_ORDER);

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
                                ps.println(RSAUtils.encrypt(
                                        String.format(getFormat(FORMAT_NOTE_CONTENT), phoneNumber)));
                            }
//                            // Print call date
//                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
//                                    .format(mContext.getString(R.string.format_datetime_mdhm),
//                                            callDate)));
                            // Print call attachment location
                            if (!TextUtils.isEmpty(location)) {
                                ps.println(RSAUtils.encrypt(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location)));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(RSAUtils.encrypt(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content)));
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
        public int exportToText(boolean isShare) {
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
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null,SORT_ORDER);

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
                                ps.println(("----------------"));
                                ps.println(RSAUtils.encrypt(String.format(getFormat(FORMAT_FOLDER_NAME), folderName)));
                                ps.println(("----------------"));
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
            ps.println(("----------------"));
            ps.println(RSAUtils.encrypt(String.format(getFormat(FORMAT_FOLDER_NAME), DEFAULT)));
            ps.println(("----------------"));
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
            if (isShare)
                shareFile(mContext);
            return STATE_SUCCESS;
        }


        public static void shareFile(Context context) {
            Uri uri = Uri.fromFile(generateFileMountedOnSDcard(context, R.string.file_path,
                    R.string.file_name_txt_format));
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uri); // 添加附件，附件为file对象
            if (uri.toString().endsWith(".gz")) {
                intent.setType("application/x-gzip"); // 如果是gz使用gzip的mime
            } else if (uri.toString().endsWith(".txt")) {
                intent.setType("text/plain"); // 纯文本则用text/plain的mime
            } else {
                intent.setType("application/octet-stream"); // 其他的均使用流当做二进制数据来发送
            }
            context.startActivity(intent); // 调用系统的mail客户端进行发送
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

        public int exportXML(boolean isShare) {
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }
            ArrayList<String> foldIdList = new ArrayList<>();
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
                                + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, SORT_ORDER);

                if (folderCursor != null) {
                    if (folderCursor.moveToFirst()) {
                        do {
                            // Print folder's name
                            String folderName = "";
                            if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
//                                folderName = mContext.getString(R.string.call_record_folder_name);
                                continue;
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
                            serializer.endTag(null, FOLDER);
                            foldIdList.add(folderId);

                        } while (folderCursor.moveToNext());
                    }
                    folderCursor.close();
                }

//                // Export notes in root's folder
                Cursor noteCursor = mContext.getContentResolver().query(
                        Notes.CONTENT_NOTE_URI,
                        NOTE_PROJECTION,
                        NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                                + "=0", null, SORT_ORDER);
                serializer.startTag(null, FOLDER);
                serializer.startTag(null, FOLDER_NAME);
                serializer.text(DEFAULT);
                serializer.endTag(null, FOLDER_NAME);
                serializer.startTag(null, NoteColumns.ID);
                serializer.text(String.valueOf(0));
                serializer.endTag(null, NoteColumns.ID);
                serializer.endTag(null, FOLDER);
                for (String id : foldIdList) {
                    exportFolderToXML(id, serializer);
                }
                if (noteCursor != null) {
                    if (noteCursor.moveToFirst()) {
                        do {
                            serializer.startTag(null, NOTE);
                            serializer.startTag(null, CREATE_TIME);
                            serializer.text(String.valueOf(noteCursor.getLong(NOTE_COLUMN_CREATED_DATE)));
                            serializer.endTag(null, CREATE_TIME);
                            serializer.startTag(null, FIX_TIME);
//                            serializer.text(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
//                                    mContext.getString(R.string.format_datetime_mdhm),
//                                    noteCursor.getLong(NOTE_COLUMN_MODIFIED__DATE))));
                            serializer.text(String.valueOf(noteCursor.getLong(NOTE_COLUMN_MODIFIED__DATE)));
                            serializer.endTag(null, FIX_TIME);
                            serializer.startTag(null, BGCOLORID);
//                            serializer.text(String.valueOf(noteCursor.getLong(NOTE_BG_COLOR_ID)));
                            serializer.text("1");
                            serializer.endTag(null, BGCOLORID);

                            serializer.startTag(null, FOLDERID);
//                            serializer.text(String.valueOf(noteCursor.getLong(NOTE_PARENT_ID)));
                            serializer.text("1");
                            serializer.endTag(null, FOLDERID);
                            // Query data belong to this note
                            String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                            exportNoteToXML(noteId, serializer);
                            serializer.endTag(null, NOTE);
                        } while (noteCursor.moveToNext());
                    }
                    noteCursor.close();
                }
                serializer.endTag(null, ROOT);
                serializer.flush();
                //finally we close the file stream
                fileos.close();
                if (isShare)
                    shareFile(mContext);
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
                    }, SORT_ORDER);
            try {
                if (notesCursor != null) {
                    if (notesCursor.moveToFirst()) {
                        do {
                            String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                            if (noteId.length() > 0 && Integer.valueOf(noteId) < 1) {
                                continue;
                            }
                            serializer.startTag(null, NOTE);
                            serializer.startTag(null, CREATE_TIME);
                            serializer.text(String.valueOf(notesCursor.getLong(NOTE_COLUMN_CREATED_DATE)));
                            serializer.endTag(null, CREATE_TIME);
                            serializer.startTag(null, FIX_TIME);
                            serializer.text(String.valueOf(notesCursor.getLong(NOTE_COLUMN_MODIFIED__DATE)));
                            serializer.endTag(null, FIX_TIME);
                            serializer.startTag(null, ALERT_TIME);
                            serializer.text("0");
                            serializer.endTag(null, ALERT_TIME);
                            serializer.startTag(null, BGCOLORID);
                            serializer.text("1");
                            serializer.endTag(null, BGCOLORID);

                            serializer.startTag(null, FOLDERID);
                            serializer.text(String.valueOf(notesCursor.getLong(NOTE_PARENT_ID)));
                            serializer.endTag(null, FOLDERID);
                            serializer.startTag(null, DESKTOPX);
                            serializer.text("0");
                            serializer.endTag(null, DESKTOPX);
                            // Query data belong to this note

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
                    }, SORT_ORDER);

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

    public int getNoteTotalNumbers() {
        Cursor numberCursor = mContext.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                TextExport.NOTE_PROJECTION, null, null, null);
//        numberCursor.moveToFirst();
        return numberCursor.getCount();
    }
}


