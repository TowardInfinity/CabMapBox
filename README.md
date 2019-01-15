# Cab/Taxi Android App

This is a cab/taxi app for android users. It is single app for both Customer and Driver.
By providing two general options as Customer or Driver one can decide the type.
This app provides a login and registration support where a user can first decide type then proceed. So multiple users at once can be accommodated. **Mapbox SDK** is used for Map. And for **Realtime-Location** Services, **Firebase** is used. The app was made in the thought that it can be edited easily and understood easily, and latest Android Pie is used for testing and is tested on previous versions also.

## Screenshots

![User](https://github.com/TowardInfinity/CabMapBox/blob/master/images/combined_1.png)
![UserMap](https://github.com/TowardInfinity/CabMapBox/blob/master/images/combined_2.png)
![Navigation](https://github.com/TowardInfinity/CabMapBox/blob/master/images/7_seven.png)

## Getting Started

### Using Github Project

This project uses the **Gradle** build system. To build this project, use the `gradlew build` command or use "Import Project" in **Android Studio**.

For more resources on learning Android development, visit the [Developer Guides](https://developer.android.com/guide/) at [developer.android.com](https://developer.android.com/).

### Using Mapbox

From Mapbox we need an account and access token.
Sign up for an account at [mapbox.com/signup](https://www.mapbox.com/signup/). You can find your [access tokens](https://www.mapbox.com/help/how-mapbox-works/access-tokens/) on your [Account page](https://www.mapbox.com/account/).

Copy the access token and place the token and go to **String Resources** and paste your token at **access_token**.

For more resources on learning Mapbox Map SDK, visit the [Mapbox Android](https://www.mapbox.com/help/glossary/mapbox-android-sdk/) and [Mapbox Navigation App](https://www.mapbox.com/help/tutorials/android-navigation-sdk/) at [mapbox.com](https://www.mapbox.com).

### Using Firebase

In this project, Firebase Authentication and Real-time Database are used. You don't need to do any coding for Firebase all the things have been managed. The first setup accordingly, and observe the [Console](https://console.firebase.google.com/).
You will need to setup Firebase for project as described in [Firebase to Android Project](https://firebase.google.com/docs/android/setup) at [firebase.google.com](https://firebase.google.com/).

#### Firebase Authentication

Set up an Android project for **Email and password authentication** as described in [Email Password Authentication](https://firebase.google.com/docs/auth/android/password-auth) at [firebase.google.com](https://firebase.google.com/).

#### Firebase Real-Time Database

We are using [Firebase Real-Time Database](https://firebase.google.com/docs/database/android/start) as it is the best present, and which fulfils our need i.e, **GeoLocation Query**. We are doing Geolocation Query with the help of [GeoFire](https://github.com/firebase/geofire-java). Use the above links for resources and learning.

