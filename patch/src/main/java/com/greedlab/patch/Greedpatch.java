package com.greedlab.patch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.greedlab.patch.utils.MD5Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Bell on 16/9/14.
 */
public class Greedpatch {
    final private static String PREFERENCE_KEY = "preference_key";
    final private static String PROJECT_VERSION_KEY = "project_version_key";
    final private static String PATCH_VERSION_KEY = "patch_version_key";
    final private static String PATCH_FILE_NAME_KEY = "patch_file_name_key";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static String serverAddress = "http://patchapi.greedlab.com";
    public static String projectId = null;
    public static String token = null;

    private static Greedpatch sInstance;

    private Context mContext;
    private String projectVersion;
    private int patchVersion;
    private String patchFileName;

    private void setProjectVersion(String projectVersion) {
        SharedPreferences.Editor editor = getPreferencesEditor();
        editor.putString(PROJECT_VERSION_KEY, projectVersion);
        editor.commit();

        this.projectVersion = projectVersion;
    }

    public void setPatchVersion(int patchVersion) {
        SharedPreferences.Editor editor = getPreferencesEditor();
        editor.putInt(PATCH_VERSION_KEY, patchVersion);
        editor.commit();
        this.patchVersion = patchVersion;
    }

    public void setPatchFileName(String patchFileName) {
        SharedPreferences.Editor editor = getPreferencesEditor();
        editor.putString(PATCH_FILE_NAME_KEY, patchFileName);
        editor.commit();

        this.patchFileName = patchFileName;
    }

    private Greedpatch(Context context) {
        this.mContext = context;
        SharedPreferences sharedPref = getPreferences();
        this.projectVersion = sharedPref.getString(PROJECT_VERSION_KEY, null);
        this.patchVersion = sharedPref.getInt(PATCH_VERSION_KEY, 0);
        this.patchFileName = sharedPref.getString(PATCH_FILE_NAME_KEY, null);
    }

    public static synchronized Greedpatch getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Greedpatch(context);
        }
        return sInstance;
    }

    /**
     * request new patch
     */
    public void requestPatch() {
        if (this.projectId == null) {
            System.out.println("projectId can not be null");
            return;
        }
        if (this.serverAddress == null) {
            System.out.println("serverAddress can not be null");
            return;
        }
        if (this.token == null) {
            System.out.println("token can not be null");
            return;
        }

        // url
        final String url = serverAddress + "/patches/check";

        // header
        final Map<String, String> headersMap = new HashMap<String, String>();
        headersMap.put("Content-Type", "application/json; charset=utf-8");
        headersMap.put("Accept", "application/vnd.greedlab+json;version=1.0");
        headersMap.put("Authorization", "Bearer " + this.token);

        // body
        final JSONObject jsonObject = new JSONObject();
        try {
            String currentVersion = getVersionName();
            if (currentVersion.equals(this.projectVersion) && this.patchVersion != 0) {
                jsonObject.put("patch_version", this.patchVersion);
            }
            jsonObject.put("project_version", currentVersion);
            jsonObject.put("project_id", projectId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jsonOutput = jsonObject.toString();
        Log.d("greedpatch", jsonOutput);

        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(JSON, jsonOutput);
        Headers headers = Headers.of(headersMap);
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("greedpatch", "request patch failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int code = response.code();
                if (code == 200) {
                    String stringBody = response.body().string();
                    Log.d("greedpatch", stringBody);
                    JSONObject body = null;
                    try {
                        body = new JSONObject(stringBody);
                        String patchUrl = body.getString("patch_url");
                        if (patchUrl == null || "".equals(patchUrl)) {
                            return;
                        }
                        final String projectVersion = body.getString("project_version");
                        if (projectVersion == null || "".equals(projectVersion)) {
                            return;
                        }
                        final int patchVersion = body.getInt("patch_version");
                        if (patchVersion == 0) {
                            return;
                        }
                        final String hash = body.getString("hash");
                        if (hash == null || "".equals(hash)) {
                            return;
                        }
                        String filename = patchUrl.substring(patchUrl.lastIndexOf('/') + 1);
                        if (filename == null || "".equals(filename.trim())) {
                            filename = "patch.jar";
                        }

                        File patchDir = getPatchDirectory(projectVersion, Integer.toString(patchVersion));
                        if (patchDir == null || !patchDir.exists()) {
                            Log.e("greedpatch", "make patch directory failed!");
                            return;
                        }
                        final File file = new File(patchDir, filename);
                        final String finalFilename = filename;
                        downloadPatch(patchUrl, file, new DownloadCallBack() {
                            @Override
                            public void downloadSuccess() {
                                Log.d("greedpatch", String.valueOf(file.length()));
                                String patchHash = MD5Util.getFileMD5(file);
                                if (patchHash == null || !patchHash.equals(hash)) {
                                    Log.e("greedpatch", "wrong hash: " + patchHash + " to " + hash);
                                    return;
                                }
                                setPatchFileName(finalFilename);
                                setProjectVersion(projectVersion);
                                setPatchVersion(patchVersion);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (code == 204) {
                    Log.d("greedpatch", "no need to patch");
                } else {
                    Log.d("greedpatch", "request patch failed");
                }
            }
        });
    }

    /**
     * whether need patch
     *
     * @return
     */
    public Boolean needPatch() {
        if (this.projectVersion == null || this.patchVersion == 0) {
            return false;
        }
        String currentVersion = getVersionName();
        if (currentVersion.equals(this.projectVersion)) {
            return true;
        }
        return false;
    }

    /***
     * get current patch file
     *
     * @return
     */
    public File getPatchFile() {
        File patchDir = getPatchDirectory();
        if (patchDir == null || this.patchFileName == null) {
            return null;
        }
        File file = new File(patchDir, this.patchFileName);
        return file;
    }

    public interface DownloadCallBack {
        public void downloadSuccess();
    }

    private void downloadPatch(String downloadUrl, final File file, final DownloadCallBack callBack) {
        if (downloadUrl == null || "".equals(downloadUrl)) {
            return;
        }
        if (file == null) {
            return;
        }
        Log.d("greedpatch", file.getAbsolutePath());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("greedpatch", "download failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        Log.d("greedpatch", "progress=" + progress);
                    }
                    fos.flush();
                    Log.d("greedpatch", "download successful!");
                    callBack.downloadSuccess();
                } catch (Exception e) {
                    Log.d("greedpatch", "download failed!");
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * get current patch directory
     *
     * @return
     */
    private File getPatchDirectory() {
        if (!needPatch()) {
            return null;
        }
        return getPatchDirectory(this.projectVersion, String.valueOf(this.patchVersion));
    }

    /**
     * get patch directory
     *
     * @param projectVersion
     * @param patchVersion
     * @return
     */
    private File getPatchDirectory(String projectVersion, String patchVersion) {
        if (projectVersion == null && patchVersion == null) {
            return null;
        }
        String child = "patch/" + projectVersion + "/" + patchVersion;
        File directory = new File(this.mContext.getExternalFilesDir(null), child);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    /**
     * get version
     *
     * @return
     */
    private String getVersionName() {
        String versionName = "1.0.0";
        PackageManager packageManager = this.mContext.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.mContext.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }

    /**
     * get SharedPreferences
     *
     * @return
     */
    private SharedPreferences getPreferences() {
        return this.mContext.getSharedPreferences(
                PREFERENCE_KEY, Context.MODE_PRIVATE);
    }

    /**
     * get SharedPreferences Editor
     *
     * @return
     */
    private SharedPreferences.Editor getPreferencesEditor() {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.edit();
    }

}
