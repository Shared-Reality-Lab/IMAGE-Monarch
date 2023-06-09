# IMAGE-Monarch
### Contents
- [Introduction](#introduction)

- [Getting started](#getting-started)
  - [How do I install it on my Monarch?](how-do-i-install-it-on-my-monarch-from-the-repo)
  - [How do I use the application?](how-do-i-use-the-application)

- [Details...](#details)
  - [Tactile graphics](#tactile-graphics)
  - [Develop, debug, improve!](#develop-debug-improve)


## Introduction
This is the source code for an Android application to render tactile graphics on the Monarch. The application works by reading graphic files from the device file system (or using the coordinates entered for maps), making requests to the [IMAGE server](https://github.com/Shared-Reality-Lab/IMAGE-server) and rendering the responses as tactile graphics on the pin array.

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
The application UI visually appears as shown below:
![Monarch application GUI](https://github.com/Shared-Reality-Lab/IMAGE-Monarch/assets/53469681/c3b4327e-e1f8-404a-9470-79b48ff87476)

**DOWN**: Lowers all the raised pins \
**UP**: Raises the pins of the next available layer of the tactile graphic. You can loop through the sequence of layers in the tactile graphic by repeatedly pressing the UP button. (After you press the UP button, the pins corresponding to the layer are raised almost instantly. However, there is a lag in loading the TTS labels associated with the objects in each layer. A ping will play when the TTS labels are successfully loaded.) \
**DebugView**: Shows/hides the debug view i.e. the visual display of the pins. \
**Text Fields**: The two text fields help you to make dynamic server requests for the map of any desired POI. You will need to enter the latitude and longitude coordinates of the POI in the first and second text fields respectively. \
**GET MAP!**: Sends a request to the server for the latitude and longitude coordinates of the POI entered in the text fields. 

Use the directional buttons on the Monarch to navigate through the buttons and fields on the UI. Press the 'confirm' button to click on a button.
Use the Up and Down arrows on the device to navigate between the files in the target directory.

## Details...
### Tactile graphics
The tactile graphic to be rendered on the device is received in SVG format. Using SVGs makes the renderings independent of the form factor of the pin array. It also allows for the tags/descriptions associated with each object or region in the graphic to be defined within the SVG and simple implementation of features like layering and zooming (not supported yet!).
Further, a format has been defined for the tactile graphic rendering SVGs. This ensures that as long as the format guidelines are followed, the application should be capable of rendering the tactile graphic thus making it extensible to other graphics (beyond photos and maps) while keeping the client side code light. These guidelines were defined by taking inspiration from the [DAISY Accessible Publishing Knowledge Base](http://kb.daisy.org/publishing/docs/html/svg.html)

The rendering SVGs must comply with the following guidelines:
- Layers must be indicated by extending the data-* attribute to include data-image-layer.
- Elements that have the data-image-layer attribute must be treated as part of the specified layer.
- Elements that do not have the data-image-layer attribute but are descendants of an element or elements with the attribute must be treated as part of the layer of specified in their closest ancestor.
- If this attribute is specified but certain visible elements do not have this attribute and are not a descendant of an element with this attribute, they must only appear in a "full picture" overview showing all layers.
- If this attribute is not specified on any elements within the SVG, the graphic should be treated as not containing layers.
- Elements that are in the same layer should be grouped under a g tag.
- Labels for a layer/ element within a layer should be indicated using an aria-label attribute.
- Labels may be specified using a desc attribute that is referenced from elements in the layer using aria-labelledby.
- The names of layers must be space-separated tokens as defined in the [HTML spec](https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#set-of-space-separated-tokens).
- Long descriptions for an element should be indicated using an aria-description attribute.
- Long descriptions may be specified using a desc attribute that is referenced from elements in the layer using aria-describedby

Should/must/may used here are as per [RFC 2119](https://www.rfc-editor.org/rfc/rfc2119).

### Develop, debug, improve!
Refer this section for an overview of the program flow to get you started... 

The flowcharts indicate the sequence of functions called when you interact with the elements of the UI. The list beside each block provide the sequential order of various actions executed by each function. Function calls/ important code segments within each function are indicated by a cascade of blocks from the calling function. 

While the functions called return values in most cases, this has not been made explicit by the arrows.  

![Server request flow](https://github.com/Shared-Reality-Lab/IMAGE-Monarch/assets/53469681/e1b88a4c-22af-4e60-b1d6-1108a334a11e)
![Load layer flow](https://github.com/Shared-Reality-Lab/IMAGE-Monarch/assets/53469681/8c62d103-1f3a-4f43-a6a6-1606a82e06e4)
