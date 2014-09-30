Translation Studio 2.0
--

translationStudio for unfoldingWord


###Contributing
Below are the technical specifications regarding this Android app and instructions for setting up your development environment.

* **Minimum SDK**: 15 (4.0.3 Ice Cream Sandwich)
* **Target Devices**: Nexus 7, 10" Tablets
* **IDE**: [Android Studio]
* **Emulator**: [Genymotion] (way better than the default android emulator)

First you need to fork the repository! Go ahead and do so now.

Next before you can start developing you must install [Android Studio]. 15 is currently the standard minimum sdk version so it is likely you will have it once [Android Studio] has been installed. If not then you will need to download it following the instructions in [Adding SDK Packages].

Next install [Genymotion] and build an emulator. There are a few different licenses available, just choose the free license. Once installed the interface is pretty straight forward so if you experience difficulty setting up an emulator please refer to their documentation.

>NOTE: [Genymotion] requires [Virtual Box] as a dependency. You may pre-install [Virtual Box] or download the [Genymotion] installer prepackaged with [Virtual Box].

For added convenience you may download the [Genymotion Android Studio Plugin]. This will add a button next to the default AVD Manager button in your [Android Studio] menu bar. From there you can create new emulators or start existing ones.

>NOTE: I have found that the [Genymotion] emulators must be started before running the application in Android Studio.

Now you are ready to develop! Run the app in debug mode (the bug button) and watch the app pop up on your emulator. 

Write some code, add a *USEFUL* but short and sweet commit message, repeat. If you run on multiple computers make sure to push to your github repository so you can develop at home or on vacation.

###Submiting Your [Beautiful] Code
>Important! Your code will **NOT** be accepted if it does not follow the [Code Style Guidelines].
> Please become familure with the [Code Style Guidelines]. The repository is > guarded by a band of crazed-coding-style-obsessed-monkeys who will not hesitate to deny your pull request if you so much as have an extra space at the end of a line.

Once you are ready to share you code (meaning it works and is written beautifully) send us a pull request. You can do so from your repository page in GitHub.


###Additional Info
Please check out the wiki for additional documentation to help you get started in the development.

[Virtual Box]:https://www.virtualbox.org/
[Genymotion Android Studio Plugin]:https://cloud.genymotion.com/page/doc/#collapse-intellij
[Adding SDK Packages]:http://developer.android.com/sdk/installing/adding-packages.html
[Genymotion]:http://www.genymotion.com/
[Android Studio]:https://developer.android.com/sdk/installing/studio.html
[Code Style Guidelines]:https://source.android.com/source/code-style.html
