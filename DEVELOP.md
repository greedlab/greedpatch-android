# DEVELOP

## Network

* request patch with [Transmitting Network Data Using Volley](https://developer.android.com/training/volley/index.html)
* install volley from [JCenter](https://bintray.com/android/android-utils/com.android.volley.volley)
* download file with [DownloadManager](https://developer.android.com/reference/android/app/DownloadManager.html)

## Storage

* save project version and patch version with [SharedPreferences](https://developer.android.com/training/basics/data-storage/shared-preferences.html)
* save file with [Files](https://developer.android.com/training/basics/data-storage/files.html)

## Plugins

add

```
plugins {
    id "com.jfrog.bintray" version "1.7"
}
```

to the header after buildscript in build.gradle of every module

### Reference

* [Gradle Plugins](https://docs.gradle.org/2.10/userguide/plugins.html)

## bintray configuration

config

```
bintray {
    user = System.getenv('BINTRAY_USER')
    key = System.getenv('BINTRAY_KEY')
    ...
}
```

 in build.gradle of every module

## Create Project

```
Android Studio > File > New > New Project > com.greedlab.greedpatch > Empty Activity
```

## Add Module

```
Android Studio > File > New > New Module > Android Library > next > com.greedlab.patch
```
