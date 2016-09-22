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
import java.io.IOException;
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
import okio.BufferedSink;
import okio.Okio;

/**
 * Created by Bell on 16/9/14.
 */
public class Greedpatch {
    private static final String PREFERENCE_KEY = "preference_key";
    private static final String PROJECT_VERSION_KEY = "project_version_key";
    private static final String PATCH_VERSION_KEY = "patch_version_key";
    private static final String PATCH_FILE_NAME_KEY = "patch_file_name_key";

    public String serverAddress;
    public String projectId;
    public String token;

    private String projectVersion;
    private int patchVersion;
    private String patchFileName;

    private static Greedpatch sInstance;

    private void setProjectVersion(Context context, String projectVersion) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putString(PROJECT_VERSION_KEY, projectVersion);
        editor.commit();

        this.projectVersion = projectVersion;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    private void setPatchVersion(Context context, int patchVersion) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putInt(PATCH_VERSION_KEY, patchVersion);
        editor.commit();
        this.patchVersion = patchVersion;
    }

    public int getPatchVersion() {
        return patchVersion;
    }

    private void setPatchFileName(Context context, String patchFileName) {
        SharedPreferences.Editor editor = getPreferencesEditor(context);
        editor.putString(PATCH_FILE_NAME_KEY, patchFileName);
        editor.commit();

        this.patchFileName = patchFileName;
    }

    public String getPatchFileName() {
        return patchFileName;
    }

    private Greedpatch(Context context) {
        SharedPreferences sharedPref = getPreferences(context);
        this.serverAddress = "http://patchapi.greedlab.com";

        this.projectVersion = sharedPref.getString(PROJECT_VERSION_KEY, null);
        this.patchVersion = sharedPref.getInt(PATCH_VERSION_KEY, 0);
        this.patchFileName = sharedPref.getString(PATCH_FILE_NAME_KEY, null);
    }

    public static synchronized Greedpatch getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Greedpatch(context);
//            sInstance = new Greedpatch(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * request new patch
     */
    public void requestPatch(final Context context) {
        if (this.projectId == null) {
            Log.e("greedpatch","projectId can not be null");
            return;
        }
        if (this.serverAddress == null) {
            Log.e("greedpatch","serverAddress can not be null");
            return;
        }
        if (this.token == null) {
            Log.e("greedpatch","token can not be null");
            return;
        }

        // url
        final String url = serverAddress + "/patches/check";

        // MediaType
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        // header
        final Map<String, String> headersMap = new HashMap<>();
        headersMap.put("Content-Type", "application/json; charset=utf-8");
        headersMap.put("Accept", "application/vnd.greedlab+json;version=1.0");
        headersMap.put("Authorization", "Bearer " + this.token);

        // body
        final JSONObject jsonObject = new JSONObject();
        try {
            String currentVersion = getVersionName(context);
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
                Log.e("greedpatch", "request patch failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                int code = response.code();
                if (code == 200) {
                    String stringBody = response.body().string();
                    Log.d("greedpatch", stringBody);
                    JSONObject body;
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

                        File patchDir = getPatchDirectory(context, projectVersion, Integer.toString(patchVersion));
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
                                setPatchFileName(context, finalFilename);
                                setProjectVersion(context, projectVersion);
                                setPatchVersion(context, patchVersion);
                                Log.i("greedpatch", "need to patch");
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (code == 204) {
                    Log.i("greedpatch", "no need to patch");
                } else {
                    Log.e("greedpatch", "request patch failed");
                }
            }
        });
    }

    /**
     * whether need patch
     *
     * @return
     */
    private Boolean needPatch(Context context) {
        if (this.projectVersion == null || this.patchVersion == 0) {
            return false;
        }
        String currentVersion = getVersionName(context);
        return currentVersion.equals(this.projectVersion);
    }

    /***
     * get current patch file
     *
     * @return
     */
    public File getPatchFile(Context context) {
        File patchDir = getPatchDirectory(context);
        if (patchDir == null || this.patchFileName == null) {
            return null;
        }
        return new File(patchDir, this.patchFileName);
    }

    public interface DownloadCallBack {
        void downloadSuccess();
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
                BufferedSink sink = Okio.buffer(Okio.sink(file));
                sink.writeAll(response.body().source());
                sink.close();
                callBack.downloadSuccess();
            }
        });
    }

    /**
     * get current patch directory
     *
     * @return
     */
    private File getPatchDirectory(Context context) {
        if (!needPatch(context)) {
            return null;
        }
        return getPatchDirectory(context, this.projectVersion, String.valueOf(this.patchVersion));
    }

    /**
     * get patch directory
     *
     * @param projectVersion project version
     * @param patchVersion patch version
     * @return
     */
    private File getPatchDirectory(Context context, String projectVersion, String patchVersion) {
        if (projectVersion == null && patchVersion == null) {
            return null;
        }
        String child = "patch/" + projectVersion + "/" + patchVersion;
        File directory = new File(context.getExternalFilesDir(null), child);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                return directory;
            } else {
                return null;
            }
        }
        return directory;
    }

    /**
     * get version
     *
     * @return
     */
    private String getVersionName(Context context) {
        String versionName = "1.0.0";
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
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
    private SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(
                PREFERENCE_KEY, Context.MODE_PRIVATE);
    }

    /**
     * get SharedPreferences Editor
     *
     * @return
     */
    private SharedPreferences.Editor getPreferencesEditor(Context context) {
        SharedPreferences sharedPref = getPreferences(context);
        return sharedPref.edit();
    }
}
