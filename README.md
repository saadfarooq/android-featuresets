Android FeatureSets Plugin
==========================

A plugin to provide a convenience DSL for feature based source sets in Android. A feature set named
'feature1' defines sources in `src/feature1/{java,res,resources,assets}` and an AndroidManifest at
`src/feature1/AndroidManifest.xml`.  While in development this feature can be part of the `debug`
source set and when development is complete, it can simply be moved over to `main` with a slight
change in the `build.gradle` file without needing to copy over files or rebase stale branches and such.
Similarly, there can be `release` only features but I expect that to be a rare case.
Features can also be configured per buildType.

Usage
-----
1. Add the following to your build.gradle

   ```groovy
   buildscript {
      repositories {
         mavenCentral()
      }

      dependencies {
         classpath 'com.github.saadfarooq:android-featuresets:{version}'
      }
   }

   repositories {
      mavenCentral()
   }

   apply plugin: 'com.android.application'
   apply plugin: 'com.github.saadfarooq.featuresets'
   ```
   alternatively, you can use the new plugin syntax for gradle `2.1+`
   ```groovy
   plugins {
      id "com.github.saadfarooq.featuresets" version "<latestVersion>"
   }
   ```

2. Define the dependencies in the featureSets closure

   ```groovy
   android {
        ...
        featureSets {
             debug {
               encapsulateTests true
               features 'featureInDevelopment', 'anotherFeature'
             }
             main {
               features 'featureComplete'
             }
        }
   }
   ``` 

The plugin checks that a corresponding build type exists for each
featureSet and adds the appropriate source folders to that sourceSet.

Test Sources
------------
By default, test source dir for featureSets are set to `src/test<featureName>` (e.g. `testFeatureInDebug`).
Using the `encapsulateTests true` option, test source dir is instead set to `src/<featureName/test`.
This causes all of the feature related code to be inside the `src/<featureName>` folder
and results in a shorter folder hierarchy in the IDE.

Manifest Merging
----------------
One of the main benefits of this plugin over simple sourceSets is the ability to
include an AndroidManifest file for each feature. For e.g. if a feature requires
a background service, this service need not be fined in the main manifest. Instead,
the feature manifest can define the service and it's merged with main manifest on build.
If the feature is deprecated, there's no cleanup to do in the main manifest.
The plugin uses `com.android.manifmerger.ManifestMerger2` from the Android gradle plugin
so you should behave just like manifest merging between flavors.

Contributing
------------
The plugin only addresses a small use case right now. There is no support for custom build types. Jni,
renderscript and shaders support is also not available. If you would think these might be useful,
please contribute.

License
-------

    Copyright 2017 Saad Farooq
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
