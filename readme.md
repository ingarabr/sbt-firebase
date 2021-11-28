# Sbt firebase

A sbt plugin to make it easy to deploy to Google Firebase. It uses the [firebase deploy api] and does not
need the firebase SDK to be installed.

## Usage

Add the plugin to `projects/plugins.sbt`

```
addSbtPlugin("com.github.ingarabr" % "sbt-firebase" % "<version>")
```

The plugin has the following tasks:

| Task | Description | 
|---|---|
| firebaseDeployHosting | Deploy files to Firebase Hosting |


## Configuration

```sbt
// Required 

// Can be the firebase project name or the firebase site-name 
// if you have multiple sites
firebaseSiteName := "firebase-project-name"

// The folder containing all the web asset files that will be deployed.
// This is the folder where you want to copy over scala-js and other
// static assets file.
firebaseHostingFolder := target.value / "public"  // example location

// Optional

// This is the firebase hosting configuration. It's the firebase rest API
// structure and it's quite similar to the firebase json file. It has some
// type safety to reduce some structural errors in the API.
firebaseVersionConfig := SiteVersionRequest.basic // default value

// How we authenticate with Google Cloud/Firebase. It uses the same default
// behaviors as all other Google java libs. You can it to specifying a
// service account file with AuthType.ServiceAccountKey(path)
firebaseAuth := AuthType.ApplicationDefault // default value
```

[firebase deploy api]: https://firebase.google.com/docs/hosting/api-deploy#raw-http-request_2
