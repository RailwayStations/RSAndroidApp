# Bahnhofsfotos
Android-App for the Project "Deutschlands-Bahnhoefe"

The App loads Data from a json - file, which contains all railway-stations without a photo in the project. The Json-File will
be created through the java-application from [Peter Storch](https://github.com/pstorch) on https://github.com/pstorch/bahnhoefe.gpx.

The project - website you will find on https://railway-stations.org and http://www.deutschlands-bahnhoefe.de.


To build the App you need a Google-Maps-Api-Key. Its located under `app/src/main/res/values/google_maps_api-template.xml`.

Another file has to be added: google-services.json. Please follow the tutorial on https://firebase.google.com/docs/android/setup.
Rename the FireBaseConstantsTemplate to FireBaseConstants and fill in your own data. 


The code is public under MIT-Licence: http://choosealicense.com/licenses/mit/
