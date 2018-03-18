[![Build Status](https://travis-ci.org/RailwayStations/RSAndroidApp.svg?branch=master)](https://travis-ci.org/RailwayStations/RSAndroidApp) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/rsandroidapp/localized.svg)](https://crowdin.com/project/rsandroidapp)

# Bahnhofsfotos
Android-App for the Project "Deutschlands-Bahnhoefe"

The App loads Data from a json - file, which contains all railway-stations without a photo in the project. The Json-File will
be created through the java-application from [Peter Storch](https://github.com/pstorch) on https://github.com/RailwayStations/RSAPI.

The project - website you will find on https://railway-stations.org and http://www.deutschlands-bahnhoefe.de.


To build the App you need a Google-Maps-Api-Key. Insert the key(s) in `app/templates/google_maps_api.xml` and copy the file into `app/src/main/res/values/`.

The google-services.json has to be exchanged with your firebase-created file. Please follow the tutorial on https://firebase.google.com/docs/android/setup.
Rename the FireBaseConstantsTemplate to FireBaseConstants and fill in your own data.
A template for the `google-services.json` can be found in `app/templates`. Copy it to `app` and set the correct api key.

API-Key: copy `app/templates/rs_api.xml` to `app/src/main/res/values/` and enter the API for the Backend Service.

Help to translate the project on [Crowdin](https://crowdin.com/project/rsandroidapp).

The code is public under MIT-Licence: http://choosealicense.com/licenses/mit/
