
RemoteLogger
===================
An Android library to periodically send device logs to a remote server by scheduling a background job for dispatching logs through a network call.
Works for android versions >= 14.

Gradle [![Download](https://api.bintray.com/packages/gurpreetsk95/RemoteLogger/remotelogger/images/download.svg)](https://bintray.com/gurpreetsk95/RemoteLogger/remotelogger/_latestVersion)
---------------------
RemoteLogger is available on JCenter. If your project does not include `jcenter()` already, add it to your project's build script.

    allprojects {
      repositories {
        jcenter()
      }
    }

Then, include the dependencies in your module's build script.

    dependencies {
      implementation 'com.gurpreetsk:remotelogger:(latest-version)'
    }

where {latest version} corresponds to published version in [![Download](https://api.bintray.com/packages/gurpreetsk95/RemoteLogger/remotelogger/images/download.svg)](https://bintray.com/gurpreetsk95/RemoteLogger/remotelogger/_latestVersion)

Setup
---------------------
You should initialize the library as early as possible in application lifecycle. The application `onCreate` might be the most logical choice.
This is done using the `initialize(context: Context, url: String, userUniqueIdentifier: String)` method, which requires context, the remote url to which the logs need to be sent to, and an identifier to uniquely identify the user.

Logs can be captured using methods provided in `RemoteLogger` class (in a similar way as android Log class).

Demo
--------------------
Check out the sample app in `app/` module.
A bare-bones server implementation can be found [here](https://github.com/GurpreetSK95/RemoteLogger-Server). You can use this to test the application/library.

License
---------------------

    Copyright 2018 Gurpreet Singh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
