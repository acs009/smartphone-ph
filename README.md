# smartphone-ph

Android app for interfacing with the smartphone-based pH sensor designed by the BIOEE group at University of California, San Diego (http://bioee.ucsd.edu). This app provides power to and communicates with the device to monitor and track the pH and temperature of user samples. It also incorporates validated quality of life questionnaires, such as the UCSD Shortness of Breath Questionnaire (SOBQ) and the CF Questionnaire Revised (CFQR) to correlate disease activity with the quantitative data collected.

## Source Code Description
databasemanager: Contains classes used by the survey system to manage the different results and store them under patient names
pH: Contains the classes that handle UI and data management for the pH measuring application
potStat: Contains the classes that handle UI and data management for the potentiostat chip
Re: Contains the core of the app, all the audio generation and processing, main UI management ect.
Survey: Builds the surveys and generates the views for the question
views: Contains a few custom view classes.

## Authors
* **Tom Phelps** - *Initial work*
