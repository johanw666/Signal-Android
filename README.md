# Signal Android 

Signal is a messaging app for simple private communication with friends.

Signal uses your phone's data connection (WiFi/3G/4G) to communicate securely, optionally supports plain SMS/MMS to function as a unified messenger, and can also encrypt the stored messages on your phone.

Currently available on the Play store and [signal.org](https://signal.org/android/apk/).

<a href='https://play.google.com/store/apps/details?id=org.thoughtcrime.securesms&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='80px'/></a>

## Contributing Bug reports
We use GitHub for bug tracking. Please search the existing issues for your bug and create a new one if the issue is not yet tracked!

https://github.com/signalapp/Signal-Android/issues

## Joining the Beta
Want to live life on the bleeding edge and help out with testing?

You can subscribe to Signal Android Beta releases here:
https://play.google.com/apps/testing/org.thoughtcrime.securesms
 
If you're interested in a life of peace and tranquility, stick with the standard releases.

## Contributing Translations
Interested in helping to translate Signal? Contribute here:

https://www.transifex.com/projects/p/signal-android/

## Contributing Code

If you're new to the Signal codebase, we recommend going through our issues and picking out a simple bug to fix (check the "easy" label in our issues) in order to get yourself familiar. Also please have a look at the [CONTRIBUTING.md](https://github.com/signalapp/Signal-Android/blob/master/CONTRIBUTING.md), that might answer some of your questions.

For larger changes and feature ideas, we ask that you propose it on the [unofficial Community Forum](https://community.signalusers.org) for a high-level discussion with the wider community before implementation.

## Contributing Ideas
Have something you want to say about Open Whisper Systems projects or want to be part of the conversation? Get involved in the [community forum](https://community.signalusers.org).

## WhatsApp Data Import

This is based on code contributed by Samuel Welten (https://github.com/jukefoxer/Signal-Android) and Wollwolke 
(https://github.com/Wollwolke/Signal-Android/tree/feature/wa-db-import). Thank you both for this.

This fork of the Signal App aims at providing a method to import one's WhatsApp conversations. It's currently still a pretty tedious process, but at least it's possible.

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

Copyright 2013-2021 Signal

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html

Google Play and the Google Play logo are trademarks of Google LLC.
