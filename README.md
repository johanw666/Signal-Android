# Signal Android 

This is a fork of Signal Android with extra options added. For a more complete description, see [Signal-JW changes](https://johanw666.github.io/Signal/signal-jw.html)

Signal is a messaging app for simple private communication with friends.

Signal uses your phone's data connection (WiFi/3G/4G) to communicate securely, optionally supports plain SMS/MMS to function as a unified messenger, and can also encrypt the stored messages on your phone.

Currently available on the Play store and [signal.org](https://signal.org/android/apk/).

## Sideloading a non-Google checked apk

Google is about to do an Apple and restrict the freedom to install any app on your own device. Fortunately there is a (complicated) way around this:
see the [Keep Android Open campaign](https://keepandroidopen.org/)

## WhatsApp Data Import

This is based on code contributed by Samuel Welten (https://github.com/jukefoxer/Signal-Android) and Wollwolke 
(https://github.com/Wollwolke/Signal-Android/tree/feature/wa-db-import). Thank you both for this.

This fork of the Signal App provides a method to import one's WhatsApp conversations. It's currently still a pretty tedious process, but at least it's possible.

### What works

* Import 1-to-1 text conversation threads.
* Import group chat conversations if a group chat with the same name is set up in the Signal App.
* Importing images and videos messages from WhatsApp chats.

### What doesn't work

* Multimedia messages other than images and videos are currently not imported.
* It's pretty slow (10 seconds per 1000 messages).

### How to do it

* Extract your unencrypted msgstore.db from your WhatsApp installation. There are several methods to do so. WhatsAppDump seems to offer a possibility that doesn't require rooting the device. A more detailed description of how to do so might be added here in the future.
* Copy the msgstore.db file to the top level directory of your internal storage
* Make an encrypted Backup of your Signal Messages using the built-in feature of the Signal App.
* Build and install this version of the Signal App and import the encrypted Backup of your signal messages.
* You might have to go to the app permission settings and give it the permission to manage all of the external storage.
* Go to Backup => Import WhatsApp to start the import.
* Be patient until it finishes.
* If you're happy with the WhatsApp import create another encrypted backup of all Signal messages.
* Install the original Signal app again and import the encrypted Backup.

Help
====
## Support
For troubleshooting and questions, please visit our support center!

https://support.signal.org/

## Documentation
Looking for documentation? Check out the wiki!

https://github.com/signalapp/Signal-Android/wiki

# Legal things
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.
The form and manner of this distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

## License

Copyright 2013-2023 Signal

Licensed under the GNU AGPLv3: https://www.gnu.org/licenses/agpl-3.0.html

Google Play and the Google Play logo are trademarks of Google LLC.
