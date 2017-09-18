using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace Camera2.Android.RNCamera2Android
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNCamera2AndroidModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNCamera2AndroidModule"/>.
        /// </summary>
        internal RNCamera2AndroidModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNCamera2Android";
            }
        }
    }
}
