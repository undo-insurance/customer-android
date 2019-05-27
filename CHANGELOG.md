# Changelog for Kustomer Android SDK

## 0.2.3
Release Date: 05/14/2019
* Optimized & Improved Performance of API calls that fetch Satisfaction Forms data.
* Disabled the ability for Customers to manually retry failed messages when vc-form is active or chat has ended
* Fixed crash & duplicate chat screen issue when app restarted by system

## 0.2.2 
**DEPRECATED - DO NOT USE**

Release Date: 05/8/2019
* Updated glide & other dependencies to the latest versions
* Fixed satisfaction form issue when VC forms are enabled

## 0.2.1
**DEPRECATED - DO NOT USE**

Release Date: 05/3/2019
* Implemented typing indicator feature
* Implemented customer satisfaction feature
* Added support to stop volume control form tracking in case of no internet
* Fixed phone validation issue in conversational assistant form
* Fixed the deleted node issue in multi-level list form
* Changes to Brand and Design

## 0.2.0
Release Date: 04/12/2019
* `identify()` & `isChatAvailable()` methods will always return callback on background thread.

## 0.1.33
Release Date: 04/09/2019
* Fixed identify callback issue

## 0.1.32
Release Date: 04/04/2019
* Fixed crash in notification due to AppCompatImageView

## 0.1.31
Release Date: 03/28/2019
* Protect data to ensure HIPAA compliance
* Added numbers option support in option list
* Fixed send button issue for android 5.0 and below
* Updated chat setting test cases
* Fixed time format issue for non-supported languages like Twi
* Removed delay when sending messages with attachment

## 0.1.30
Release Date: 03/08/2019 
* Fixed null pointer exceptions
* Optimized bitmap loading in memory
* Updated audio file from m4a to mp3
* Keeping conversational assistant form state when exiting away from the chat sdk
* Allowed chat messages with only attachments, message body not required 
* Hide attachment icon on volume control input prompts
* Romanian translation updated for "Chat Has Ended"

## 0.1.29
Release Date: 02/22/2019
* Update: Added support for multiple business hours range.
* Fix: Removed extra empty message image.


## 0.1.28
Release Date: 02/18/2019
Updates
* Changed pusher connection logic to maximum rely on pusher and removed the unnecessary API calls
* Added a callback handler for identify method
* Added 'Twi' language support
* Updated 'End Chat' translation in French, Georgian, Italian, Portuguese, Romanian, Turkish, U
kranian and Arabic
* Present support with message & form messages allowed

Fixes
* Fixed Input field issue in case of non-business hours and chat is temporarily closed and message 
received from the agent
* Fixed transparent agent icon issue.
* Fixed form message option values issue
* Won't Clear Pending Describe After Describing Conversation.

## 0.1.27
Release Date: 02/1/2019
* Fix: Updated pusher polling logic and pusher issue in case of subscription failure

## 0.1.26
Release Date: 01/31/2019
* Fix: Changed Okhttp client to a shared single instance

## 0.1.25
Release Date: 01/22/2019
* Kustomer SDK no longer requires file provider declaration in manifest
* Kustomer SDK no longer requires file_paths.xml

## 0.1.23

* Fixed null to int casting crash

## 0.1.22

* Fixed few crashes
* Made methods synchronized to avoid concurrency crashes
* Changed SDK's dependencies from compile to implementation

## 0.1.21

* Implemented upfront volume control tracking
* Fixed portugese translation issue
* Fixed repeated timer issue

## 0.1.20

* Fixed pusher logic
* Fixed 'End Chat' button translation in Spanish
* Fixed concurrency issues

## 0.1.19

* Added multi-level form messaging support

## 0.1.18

* Fixed few crashes

## 0.1.17

* Added 'swahili' language support
* Updated documentation

## 0.1.16

* Fixed apostrophe issue in strings file

## 0.1.15

* Added business hours feature

## 0.1.14

* Added hide new conversation button feature in closed chat listing

## 0.1.13

* Describe next conversation implemented
* Fixed the wrong team assignment to the conversation
* Connect to pusher if the last activity time is less than PusherConnectThreshold
* Fixed crash due to Boolean object being null

## 0.1.12

* Open chat sessions public method added

## 0.1.11

* Added prefix to resources for compatibility

## 0.1.10

* NPE issue for most recent session fixed

## 0.1.9

* Non-Ascii character in HTTP header issue fixed

## 0.1.8

* Emoji issue fixed
* Removed auto reply
* Implemented no history feature


## 0.1.7

* Fixed all warnings and errors from string files.

## 0.1.6

* Changed dependencies back from api to compile in gradle file

## 0.1.5

* Added Volume Control Form Feature
* Added End Chat Feature
* Added Custom Form Selection Feature
* Added Single Chat Session Feature
* Fixed default avatar issue
* Fixed Apostrophe issue in strings file

## 0.1.4

* Fixed a crash due to dialog.

## 0.1.3

* Added 46 more localization languages.
* Downgraded minSdkVersion to 19 (KitKat 4.4)

## 0.1.2

* Added Localization features to support devices of different languages.
* Updated Full Screen Image Viewer library
* Fixed Image path issue on older devices.
* Updated File Provider integration process.

## 0.1.1

* Fixed Gif issue in full screen viewer
* Added file Provider info in Readme file

## 0.1.0

* First release
