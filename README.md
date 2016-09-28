# greedpatch-android

Android SDK for [greedpatch](https://github.com/greedlab/greedpatch)

## Install

Maven:

```
<dependency>
  <groupId>com.greedlab</groupId>
  <artifactId>greedpatch-android</artifactId>
  <version>1.0</version>
</dependency>
```

or Gradle:

```
compile 'com.greedlab:greedpatch-android:1.0'
```

## Usage

### Generate patch

Use other library to generate patch, eg: [tinker](https://github.com/Tencent/tinker)

### upload patch

[greedpatch](http://patch.greedlab.com/) > select the project > click `Create patch` > upload zip file rom the last step, click Upload > select the `project version` , input the hash from the last step  > Create

### Config greedpatch

config greedpatch like

```
// config access token
Greedpatch.getInstance(this).token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE0NzM2NDg2MzA0ODgsImlkIjoiNTdkM2JmMmY5MDE1ZWU0N2ZjYzNjYWJhIiwic2NvcGUiOiJwYXRjaDpjaGVjayJ9.YPedieEibUgLecWDmuIVIdkY_Ra-4Qa2HeIQpE7Z_k8";
// config project ID
Greedpatch.getInstance(this).projectId = "57e0db7108e1483add770ad1";
```

#### Token

visit [Generate new token](http://patch.greedlab.com/settings/my/tokens/new) to generate it.

#### ProjectId

[greedpatch](http://patch.greedlab.com) > `Create project` > `Project Detail`. And then you can see `Project ID`

### Check need patch

```java
// request at the end of this method
Greedpatch.getInstance(this).requestPatch(this);
```

request remote server whether there are a new patch for current project version.

### Patch

Use other library to patch, eg: [tinker](https://github.com/Tencent/tinker)

```java
public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        File file = Greedpatch.getInstance(this).getPatchFile(this);
        if (file != null && file.exists()) {
            Log.d("greedpatch", "start local patch");
            // Use other library to patch, eg: [RocooFix](https://github.com/dodola/RocooFix)
        } else {
            Log.d("greedpatch", "no local patch");
        }
    }
}
```

## LICENSE

[MIT](LICENSE)
