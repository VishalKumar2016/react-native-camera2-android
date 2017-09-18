import React, { Component, PropTypes } from 'react';
import ReactNative, {
  requireNativeComponent,
  UIManager,
  StyleSheet,
  PermissionsAndroid,
  Platform,
} from 'react-native';

const NCamera2 = requireNativeComponent('Camera2', Camera2, {});

const styles = StyleSheet.create({
  camera: {
    flex: 1,
  }
});

export default class Camera2 extends Component {
  stop(callback) {
    UIManager.dispatchViewManagerCommand(
      this.getNodeHandle(),
      UIManager.Camera2.Commands.stop,
      null
    );
  }

  record(callback) {
    UIManager.dispatchViewManagerCommand(
      this.getNodeHandle(),
      UIManager.Camera2.Commands.record,
      null
    );
  }

  image(callback) {
    UIManager.dispatchViewManagerCommand(
      this.getNodeHandle(),
      UIManager.Camera2.Commands.image,
      null
    );
  }

  getNodeHandle() {
    return ReactNative.findNodeHandle(this.refs.recorder);
  }

  render() {
    return (
      <NCamera2
        {...this.props}
        ref="recorder"
        style={[styles.camera, this.props.style]}
      />
    );
  }
}

Camera2.propTypes = {
  onRecordingStarted: PropTypes.func,
  onRecordingFinished: PropTypes.func,
  onCameraAccessException: PropTypes.func,
  onCameraFailed: PropTypes.func,
  onImageCaptureFinish: PropTypes.func,
  type: PropTypes.oneOf(['front', 'back']),
  flash: PropTypes.oneOf(['on', 'off','auto']),
  torch: PropTypes.oneOf(['torch_on', 'torch_off']),
  videoEncodingBitrate: PropTypes.number,
  videoEncodingFrameRate: PropTypes.number
};
