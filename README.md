# IMAGE-Monarch
### Contents
- [Introduction](#introduction)

- [Getting started](#getting-started)
  - [How do I install it on my Monarch?](how-do-i-install-it-on-my-monarch-from-the-repo)
  - [How do I use the application?](how-do-i-use-the-application)

- [Details...]
  - [Tactile graphics]()
  - [Develop, debug, improve!]()


## Getting started
### How do I install it on my Monarch (from the repo!)?
1. Clone this repository
```
git clone https://github.com/Shared-Reality-Lab/IMAGE-Monarch.git
```
2. Download [SVG Kit for Android](https://scand.com/products/svgkit-android/) library
To include this library, download [svg_from_different_sources_sample.zip](https://scand.com/download/products/SVGkitAndroid/svg_from_different_sources_sample.zip) from the library's website.
Extract the zip file and copy file `libsvg.aar` from `svg_from_different_sources_sample/app/libs` to `IMAGE-Monarch/app/libs`

3. Connect the device to your system and Run 'app' from Android Studio

**NOTE:**
You might will also need to do some (or all) of the following (especially for a Monarch on which this application has never been installed before):
- Install Google TTS apk. Download the apk from a reliable source and install it via adb. You might also need to make sure that the TTS Engine is selected in the device settings.
- Grant permission to the application to read from storage. Do this by running the adb command `adb shell appops set --uid com.example.hw MANAGE_EXTERNAL_STORAGE allow`
- Create a directory `/sdcard/IMAGE/client/` on the Monarch sdcard for the application to read from. The application reads files from this directory. So you will need to copy over your 'graphic' files to this location.

### How do I use the application?

## Details...
### Tactile graphics
### Develop, debug, improve!
Refer this section for an overview of the program flow to get you started... 
