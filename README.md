
# react-native-camera2-android

## Getting started

`$ npm install react-native-camera2-android --save`

### Mostly automatic installation

`$ react-native link react-native-camera2-android`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-camera2-android` and add `RNCamera2Android.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCamera2Android.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNCamera2AndroidPackage;` to the imports at the top of the file
  - Add `new RNCamera2AndroidPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-camera2-android'
  	project(':react-native-camera2-android').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-camera2-android/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-camera2-android')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNCamera2Android.sln` in `node_modules/react-native-camera2-android/windows/RNCamera2Android.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Camera2.Android.RNCamera2Android;` to the usings at the top of the file
  - Add `new RNCamera2AndroidPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNCamera2Android from 'react-native-camera2-android';

// TODO: What to do with the module?
RNCamera2Android;
```
  