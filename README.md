# IMAGE-Monarch
### Contents
- [Introduction](#introduction)

- [Getting started](#getting-started)
  - [How do I install it on my Monarch?](how-do-i-install-it-on-my-monarch-from-the-repo)


- [Details...](#details)
  - [Tactile graphics](#tactile-graphics)
  - [Develop, debug, improve!](#develop-debug-improve)
  - [How do I use the application?](how-do-i-use-the-application)

## Introduction
This is the source code for an Android application to render tactile graphics on the [Monarch](https://www.humanware.com/en-usa/monarch). The application works by reading graphic files from the device file system (or using the coordinates entered for maps), making requests to the [IMAGE server](https://github.com/Shared-Reality-Lab/IMAGE-server) and rendering the responses as tactile graphics on the pin array.

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
- Grant permission to the application to read from storage. Do this by running the adb command `adb shell pm grant ca.mcgill.a11y.image android.permission.READ_EXTERNAL_STORAGE`
- Create a directory `/sdcard/IMAGE/client/` on the Monarch sdcard for the application to read from. The application reads files from this directory. So you will need to copy over your 'graphic' files to this location.
- You may be asked for microphone permissions on the Monarch. For this, it is best to download [ScreenCopy](https://github.com/Genymobile/scrcpy) to navigate through the permissions setup.
- The graphics fetched in classroom mode (i.e. graphics published either from [Tactile Authoring Tool (TAT)](https://github.com/Shared-Reality-Lab/IMAGE-TactileAuthoring/) or IMAGE-Extension(https://github.com/Shared-Reality-Lab/IMAGE-browser)) are accessed by decrypting using the same password used by the publisher. This password needs to be configured by creating a new file app/src/main/res/values/secret.xml and entering
```
<resources>
    <string name="password">[my-password-goes-here]</string>
</resources>
```


## Details...
### Tactile graphics
The tactile graphic to be rendered on the device is received in SVG format. Using SVGs makes the renderings independent of the form factor of the pin array. It also allows for the tags/descriptions associated with each object or region in the graphic to be defined within the SVG and simple implementation of features like layering and zooming (not supported yet!).
Further, a format has been defined for the tactile graphic rendering SVGs. This ensures that as long as the format guidelines are followed, the application should be capable of rendering the tactile graphic thus making it extensible to other graphics (beyond photos and maps) while keeping the client side code light. These guidelines were defined by taking inspiration from the [DAISY Accessible Publishing Knowledge Base](http://kb.daisy.org/publishing/docs/html/svg.html)

The rendering SVGs must comply with the following guidelines:
- The SVG should have a single title node providing an overview of the graphic within the SVG.
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
The Monarch application has been modularized to make it easy to develop and include new functionality. 

The application is majorly made up of two main types of components: 
1. To select modes in which the application is running (e.g. mode that connects to the authoring tool or one that deals with photo/map experiences) and selecting content either by alphanumeric entries or looping through files on the device. We will henceforth refer to them as 'selectors' and they can be found within the [selectors](app/src/main/java/ca/mcgill/a11y/image/selectors) directory.
2. To render the svg response received from the server. While the format of the response is expected to follow the guidelines listed [above](#tactile-graphics) we might want to vary the user experience by selecting what/ how something is presented to the user or present multiple possible renderings for the same source graphic. We will henceforth refer to these components as 'renderers' and they can be found within the [renderers](app/src/main/java/ca/mcgill/a11y/image/renderers) directory.

The other components within the application directory are miscellaneous or shared components that do not find a place under the above two types. The BaseActivity, is the activity which all other activities in the application are extended from, DataAndMethods contains methods used by activities and PollingService is a service that runs in the background checking for updates in the response.

NOTE: While refactoring, all components besides the 'Annotation mode', for which there is currently no defined future purpose, were moved into the current application. However, if you wish to refine or try out the Annotation mode its latest version can be found on the ['voice-interface'](/Shared-Reality-Lab/IMAGE-Monarch/tree/voice-interface) branch
#### Getting started: Add your own 'selector' and 'renderer' (+BONUS!: Use an existing 'renderer')
##### Creating a 'selector'
1. Copy the layout file activity_my_own_selector.xml from the 'starter_code' directory into 'app\src\main\res\layout'. Also, copy the MyOwnSelector.java file  into the application's '[selectors](app/src/main/java/ca/mcgill/a11y/image/selectors)' directory. 
2. Within activity_my_own_selector.xml, create two buttons. Set their ids to '@+id/classic' and '@+id/fill' and names to "Classic" and "Fill" respectively. 
3. Set layout, add button click and focus change listeners by copying the following into the indicated positions in MyOwnSelector.java Activity:

Code Snippet 1
```
    setContentView(R.layout.activity_my_own_selector);

        ((Button) findViewById(R.id.classic)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.classic)).setOnFocusChangeListener(focusListener);
        ((Button) findViewById(R.id.fill)).setOnKeyListener(btnListener);
        ((Button) findViewById(R.id.fill)).setOnFocusChangeListener(focusListener);
```
Code Snippet 2

```
    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener(){
        @Override
        public void onFocusChange(View view, boolean b) {
            switch (view.getId()){
                case R.id.classic:
                    speaker("Classic");
                    break;
                case R.id.fill:
                    speaker("Fill");
                    break;
            }
        }
    };
    private View.OnKeyListener btnListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int i, KeyEvent keyEvent) {
            if (keyEvent.getKeyCode()== DataAndMethods.confirmButton &&
                    keyEvent.getAction()== KeyEvent.ACTION_DOWN){
                Intent myIntent = null;
                if ((findViewById(R.id.classic)).hasFocus()){
                    //myIntent = new Intent(getApplicationContext(), BasicPhotoMapRenderer.class);
                    DataAndMethods.speaker("Switching to Classic mode");
                }
                else if ((findViewById(R.id.fill)).hasFocus()){
                    //myIntent = new Intent(getApplicationContext(), MyOwnRenderer.class);
                    DataAndMethods.speaker("Switching to Fill mode");

                }
                //myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //getApplicationContext().startActivity(myIntent);
            }
            return false;
        }};

```
4. Add Activity MyOwnSelector in AndroidManifest.xml within the application node:
```
<activity
            android:name=".selectors.MyOwnSelector"
            android:exported="true" />
```
5. Now add an additional button in activity_mode_selector.xml layout to make your selector accessible through mode selector by including the following snippet within the TableLayout element.
```
        <TableRow
            android:layout_width="match_parent"
            android:layout_height="match_parent" >

            <Button
                android:id="@+id/my_mode"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="My Mode" />
        </TableRow>
```
6. Add button click and focus change listeners by copying the following into the indicated positions in ModeSelector.java Activity:

Code Snippet 1
```
((Button) findViewById(R.id.my_mode)).setOnKeyListener(btnListener);
((Button) findViewById(R.id.my_mode)).setOnFocusChangeListener(focusListener);
```
Code Snippet 2
```
case R.id.my_mode:
    speaker("My Mode");
    break;
```
Code Snippet 3
```
else if ((findViewById(R.id.my_mode)).hasFocus()){
    myIntent = new Intent(getApplicationContext(), MyOwnSelector.class);
    DataAndMethods.speaker("Switching to My mode");
}
```
6. That's it! Run the application. A new button 'My mode' will be added within Mode Selector from where you will be able to access your new selector. 

##### Using an existing 'renderer': Making the 'Classic' button functional
7. (After completing steps 1. to 6.) If you wish to have the same end user experience as the basic photos and maps experience, you should reuse the existing BasicPhotoMapRenderer. To do so, in within MyOwnSelector.java uncomment the following lines to launch the BasicPhotoMapRenderer when Classic button is pressed
```
myIntent = new Intent(getApplicationContext(), BasicPhotoMapRenderer.class);
```
```
myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
getApplicationContext().startActivity(myIntent);
```
8. The 'Classic' button is now functional! On running the app and pressing the "Classic" button, you will see a circle of raised pins.

NOTE: Typically the 'selector' makes a server request to receive a new rendering(s) based on the request made (that is, based on the photo selected in Photo mode or the map coordinates provided in Map mode). However, for the purpose of this tutorial, the makePseudoServerCall function called at the start of activity MyOwnSelector changes the graphic to a predefined static value without actually making a call to the server. 

##### Creating a new 'renderer': Making the Fill button functional
9. (After completing steps 1. to 6.(and optionally 7. and 8.) ) We will now make a basic renderer which shows the filled in version of closed shapes! (Read NOTE at the end of this subsection) Start by copying the MyOwnRenderer.java file  into the application's 'renderers' directory. 
10. Add Activity MyOwnRenderer in AndroidManifest.xml within the application node:
```
<activity
            android:name=".renderers.MyOwnRenderer"
            android:exported="true" />
```
11. Add a new method at the end of DataAndMethods class which fills in the shape within the existing graphic

```
public static void fillShape() throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        Document doc = getfreshDoc();
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeslist=(NodeList)xPath.evaluate("//*[not(ancestor-or-self::*[@display]) and not(descendant::*[@display]) and (not(self::*[@data-image-layer]) or not(child::*))  and ((self::*[@aria-labelledby] or self::*[@aria-label]) or parent::*[@data-image-layer])]", doc, XPathConstants.NODESET);        // temporary var for objects tags
        for(int i = 0 ; i < nodeslist.getLength() ; i ++) {
            Node node = nodeslist.item(i);
            ((Element)node).setAttribute("fill", "black");
        }
        image = getStringFromDocument(doc);
    }
```
12. Add in the following lines in the onResume() function of  MyOwnRenderer to first fill in the shape within the existing svg and then raise the pins to display the graphic.

Code Snippet
```
DataAndMethods.fillShape();
DataAndMethods.displayGraphic(DataAndMethods.confirmButton, "Exploration");
```
13. Uncomment the following lines in MyOwnSelector.java to set the intent when "Fill" button is pressed
```
import ca.mcgill.a11y.image.renderers.MyOwnRenderer;
```
```
myIntent = new Intent(getApplicationContext(), MyOwnRenderer.class);
```
14. The Fill button is now functional... Run the application! 

NOTE: Despite it being executed as a separate activity, the underlying functions used to run MyOwnRenderer and end user experience are the same as that for BasicPhotoMapRenderer... Some liberties have been taken for the purpose of writing this tutorial, however typically, you should reuse BasicPhotoMapRenderer here! After you've gone through this tutorial, you should be able to figure out how to do this on your own...

--Documentation is somewhat outdated after this point--

### How do I use the application?
The application UI visually appears as shown below:
![Monarch application GUI](https://github.com/Shared-Reality-Lab/IMAGE-Monarch/assets/53469681/5223165a-6b75-4595-b403-e8b9fe176d51)

**DOWN**: Lowers all the raised pins \
<a name="UpButton"> **UP** </a>: Raises the pins of the next available layer of the tactile graphic. You can loop through the sequence of layers in the tactile graphic by repeatedly pressing the UP button. (After you press the UP button, the pins corresponding to the layer are raised almost instantly. However, there is a lag in loading the TTS labels associated with the objects in each layer. A ping will play when the TTS labels are successfully loaded.) \
**DebugView**: Shows/hides the debug view i.e. the visual display of the pins. \
**Text Fields**: The two text fields help you to make dynamic server requests for the map of any desired POI. You will need to enter the latitude and longitude coordinates of the point of interest (POI) in the first and second text fields respectively. \
**GET MAP!**: Sends a request to the server for the latitude and longitude coordinates of the POI entered in the text fields. 

Use the directional buttons on the Monarch to navigate through the buttons and fields on the UI. Press the 'confirm' button (i.e. the Enter/ dot 8 in a Perkins style keyboard) to click on a button.
Use the Up and Down arrows on the device to navigate between the files in the target directory.

Refer this section for an overview of the program flow to get you started... 

The flowcharts indicate the sequence of functions called when you interact with the elements of the UI. The list beside each block provides the sequential order of various actions executed by each function. Function calls/ important code segments within each function are indicated by a cascade of blocks from the calling function. 

While the functions called return values in most cases, this has not been made explicit by the arrows.  

1. Functions executed when a file is read from storage (by pressing the Up and Down arrow buttons on the device )
![Server_request_flow](https://github.com/Shared-Reality-Lab/IMAGE-Monarch/assets/53469681/51b0b946-025f-4d9d-b01a-d5ddcda5e1bd)

2. Functions executed to render the next layer (when the [UP button](#UpButton) is pressed)
![Layer_load_flow](https://github.com/Shared-Reality-Lab/IMAGE-Monarch/assets/53469681/c340412c-8ccb-45b8-a3f8-1ab1d12dfe65)

Details of the XPath queries can be found in [here](XPathQueries.md).
