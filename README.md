ReadingLog
==========

Parents can manage reading logs for kids using the bar code scanner.

Description
-----------

The app enables parents to manage reading logs of books for kids. Books can be scanned in via the barcode scanner on the phone or entered manually. Book data is retrieved from a public Google API, including a thumbnail image of the book. A parent can store additional data about the book, including:
- date read
- time spent reading in minutes
- who read this: child , parent or "read together" 
- comment for storing additional information

Parents can e-mail an HTML report in order to print out the log to submit to the kids' teachers. Multiple logs can be managed simultaneously.

It has been developed on Android 2.3 with a Droid X and compatibility-wise should work on any Android 2.1+ device with a camera. It relies on the zxing Barcode Scanner application to scan books. If you don't have that app, the phone should prompt you to download it the first time you attempt to scan a book. Book information can also be entered manually, in case the scan fails or there is no bar code (should be rare).

You'll need some books to scan. It should be pretty self-explanatory to get started, once the app is launched. You can also:

* long press a book in the log in order to add/edit book data or remove the book
* use the options menu to scan a book, enter book information manually, share the log, switch to a different log, create new logs, edit the name of the selected log or delete a log.

Note: An advert may display after sending the log.

Release Notes
-------------
### Version 1.2 (4/17/2015)
* Upgraded to latest Android Menu Strategy to better support latest Android OS versions.
* Fixed NPE when loading JSON
* Displays "time not tracked" instead of "0 minutes"
* Adds new default "Read By" metric: "Read By Me"

### Version 1.1 (1/7/2013)

This version adds some prioritized features, which have been requested by Users. It also adds cleanup for ICS and Jellybean devices and a few bug fixes. Your existing logs will be unharmed by this update.

#### New Features

* Short press a book entry in the log to see a detail screen.
* Ability to view and edit the date read on the detail screen.
* Ability to add and adjust minutes read, in order to track time spent reading on the detail screen.
* Ability to add and update a comment field.
* Added all new fields to the HTML report.

#### Bugs and Improvement

* UI touchup for a cleaner experience.  
* Adjusted log entry display to include the date read, instead of the read by information. 
* Resolved network access on main activity thread, when downloading book data.
* Display of scaled images is more consistent between devices supporting different densities.

#### Minor Releases

* Version 1.1.2 (1/7/2013) - Fixed defect causing app crash on db upgrade as part of the 1.1 release.
* Version 1.1.3 (1/7/2013) - Minimized impact of upgrade to 1.1 (sorting, intitializing date read)
