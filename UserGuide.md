# Prerequisites:

1. Laptop/ PC with the following software installed:  
   1. Chrome web browser  
   2. adb  
   3. scrcpy (not required to run system but crucial for setup/debugging)  
2. Stable internet connection (for both the Monarch and the laptop/ PC)  
3. USB cable to connect laptop/PC to the Monarch (for launching and/or re-installing application)

# FIRST TIME LAPTOP SETUP (on a new laptop/ PC)

1. Open the Google Chrome browser and install the IMAGE Browser-Extension from the Chrome webstore: [Link to IMAGE-Extension in the webstore](https://chromewebstore.google.com/detail/image-extension/iimmeciildnckfmnpbhglofiahllkhop)  
2. In the IMAGE Extension options, turn on the ‘Monarch’ option (located within the Haptic Devices section)  
3. Additional fields appear. Paste the following data into the corresponding fields:  
   1. Channel Id: [Your own six digit code]
   2. Secret Key: [A password to restrict write access] 
   3. Encryption Key: abc (Can be modified by changing the ```password``` value in app/src/main/res/values/secret.xml) 
4. Save by clicking ‘Save Changes’. 

**Note: Entering a Channel Id or Secret Key at this stage is optional. If you choose to skip this step, a random id and key will be assigned to you by the server when you send content to the Monarh as described in the subsequent section. You will then need to modify ```share_code``` in app\src\main\res\values\setup.xml to match the Channel Id assigned to you and install the application again on the Monarch.**

# SENDING CONTENT TO THE MONARCH:

1. Send photo, map, or textbook diagram to the Monarch:  
   * Photograph  
     1. On laptop, navigate to a webpage with a photo (examples below)  
     2. If you are using a screen reader:  Select ‘Interpret this graphic with IMAGE’ button  
     3. Shortcut if you are not using a screen reader: use the context menu: right mouse click on the photo \> Interpret this graphic with IMAGE.  
     4. Content: You can select content from the examples below or use content you find on the web.   
   * Photos:  
     	Bird photo: [https://images.pexels.com/photos/1126384/pexels-photo-1126384.jpeg](https://images.pexels.com/photos/1126384/pexels-photo-1126384.jpeg)  
     \[Cows: [https://www.myprincegeorgenow.com/191397/news/rapid-weather-changes-prove-tough-for-northern-bc-cattle-ranchers/](https://www.myprincegeorgenow.com/191397/news/rapid-weather-changes-prove-tough-for-northern-bc-cattle-ranchers/), Soccer kids: [https://www.gooddayorangecounty.com/5-traits-kids-learn-from-playing-soccer-at-a-young-age/](https://www.gooddayorangecounty.com/5-traits-kids-learn-from-playing-soccer-at-a-young-age/)\]  
   * Map:  
     1.  [https://image.a11y.mcgill.ca/pages/maps\_demo.html](https://image.a11y.mcgill.ca/pages/maps_demo.html)   
     2. Type in address and click search.  
     3. Click the ‘Interpret this map with IMAGE’ button below the embedded map.  
   * Textbook diagram:    
     1. Use same procedure as for photographs  
   * Once the request is processed an IMAGE Extensions Rendering pop up appears. Within this list, select the option that starts with ‘Tactile rendering of photo/ map/ multistage diagram ...’ for photos, maps and textbook diagrams respectively and then click ‘Send to Monarch’ among the options that appear.   
2. Access graphics on the Monarch application   
   * Make sure that the Monarch is turned on and connected to the laptop/ PC via a USB cable. It should show up as a USB device connected to your system. The volume on the device should be turned up to hear the TTS.  
   * Open Command Prompt/ Terminal on Mac and execute the following adb command to launch the app on the Monarch:  
     ```adb shell am start -n ca.mcgill.a11y.image/ca.mcgill.a11y.image.selectors.ModeSelector```  
     **Note: You can use the same command to relaunch the application if you accidentally exit it or the device stops responding.**  
   * Once launched, use the up and down buttons on either of the D-pads to navigate the ‘modes’. When the TTS reads ‘Classroom Mode’ press the ‘confirm’ button (The dot-8 button/ right most button on the Perkins style keyboard).   
   * Use the up and down buttons on the D-pad until the TTS reads ‘Exploration mode’ and press the ‘confirm button’.  
   * **Note: if you select a wrong option, you can return to the previous menus by using the back button (triangular button on the bottom edge of the device i.e., the side facing you).**   
   * After a short pause, the graphic you have sent using the extension (in the earlier step) will appear on the Monarch display.   
   * Any subsequent graphics sent via the Extension will also appear on this display after a short delay.  
   * **Note: after one hour, any graphics sent will be cleared and will need to be sent again.**  
3. Navigating the IMAGE-Monarch application  
   * Short audio labels: Press your finger on/inside an object being displayed and press the ‘select’ button (the small circular, orange button at the center of the device). This will read out the audio label associated with the object.  
   * Long description: If a long description is available, a beep will play at the end of the short audio label. Press on/ within the object and press ‘select’ for a longer time.  
   * Layers: Each graphic is generally composed of one or more layers. Move to next layer: dot-8 button/ right most button on the Perkins style keyboard  
     Move to previous layer: the dot-7 button/ left most button  
     Layers will loop back to the first layer when you reach the end.  
   * Zooming: Press ‘Zoom in’ (button with ‘+’ sign). TTS reads out ‘Zoom mode enabled’. Point and click at any point within the graphic to zoom in. Repeat the point and click until the desired zoom level is achieved. Disable the zoom mode by pressing the ‘Zoom in’ button again. TTS reads out ‘Zoom mode disabled’. You can zoom out using the same process but starting by pressing ‘Zoom out’ instead.   
   * Verbal queries: You can verbally ask for additional information regarding the graphic by pressing and holding the ‘recent’ (square button on the bottom edge of the device i.e. the side facing you) until you hear a beep. **Note: make sure you hear the beep before you release the button or you will accidentally exit the application\!** Following the beep, you can say something like “What colours are in the photo?”. **Note: You will need to be in a reasonably quiet space for the speech recognition to work.** After a short pause, the TTS says “Query received....”. Press ‘confirm’ to make the query or ‘cancel’ (dot-7/ right-most button) to cancel it. 

# SENDING CONTENT TO THE MONARCH via TAT (not accessible\!):

1. Instead of selecting ‘Send to Monarch’ select ‘Send to Tactile Authoring Tool (TAT)’ in step 1 of [SENDING CONTENT TO MONARCH](#sending-content-to-the-monarch) section.   
2. The TAT opens in a new tab and prompts for a password. Type abc and click ‘OK’.  
3. Customize the SVG using the drawing tools.   
4. Click ![Publish tool icon](https://github.com/user-attachments/assets/5df21331-36c6-402f-ac78-f63184b80bb8) on the side toolbar and then ”Submit" in the pop-up window to send the customized content to the Monarch. 

# FAQ:

1. **We are doing demos on two different Monarchs simultaneously, and they are interfering\!**

   This is occurring because both the Monarchs are subscribed to the same share code. This means that both the Monarchs will show the last update made to that code. 

   You can change the code by modifying ```share_code``` in app\src\main\res\values\setup.xml to match the Channel Id assigned to you and install the Android application again on the Monarch. 


2. **The IMAGE extension not activating\!**  
   Try refreshing the webpage, and activate it again.  
   Try closing and relaunching the browser.  
   Worst case: uninstall and reinstall the browser extension (you will have to reconfigure the Monarch link if you do this\!)  
     
3. **The Monarch application is not responding**

   You might have accidentally exited the application, or it might have crashed.

* To confirm if the app has crashed, open scrcpy. If you see a message like ‘Application has stopped responding’ close the app using  scrcpy. If you see the home screen/ any other screen instead, you might have accidentally exited the application.   
* Use adb to launch the IMAGE-Monarch System as described in [SENDING CONTENT TO MONARCH](#sending-content-to-the-monarch) section (step 2).  
