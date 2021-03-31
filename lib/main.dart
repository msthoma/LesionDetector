import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:tflite/tflite.dart';

List<CameraDescription> cameras;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    cameras = await availableCameras();
  } on CameraException catch (e) {
    print("Error ${e.code}\nError msg: ${e.description}");
  }
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(),
      home: InferencePage(cameras: cameras),
    );
  }
}

/// Responsible for loading Tflite model.
class InferencePage extends StatefulWidget {
  final List<CameraDescription> cameras;

  const InferencePage({Key key, @required this.cameras}) : super(key: key);

  @override
  _InferencePageState createState() => _InferencePageState();
}

class _InferencePageState extends State<InferencePage> {
  ModelState _modelState = ModelState.loading;

  @override
  void setState(VoidCallback fn) {
    // check if still mounted before setting state
    if (mounted) {
      super.setState(fn);
    }
  }

  @override
  void initState() {
    super.initState();
    initModel();
  }

  void initModel() async {
    // CNN model is loaded here
    try {
      String res = await Tflite.loadModel(
        model: "assets/cnn.tflite",
        labels: "assets/cnn_labels.txt",
      );
      print("$res loading model!");
      setState(() {
        if (res == "success") {
          _modelState = ModelState.loadSuccess;
        } else {
          _modelState = ModelState.loadError;
        }
      });
    } on PlatformException {
      print('Failed to load model.');
      setState(() {
        _modelState = ModelState.loadError;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Lesion detector")),
      body: (() {
        // set app body depending on whether the CNN has been loaded
        switch (_modelState) {
          case ModelState.loading:
            return Center(child: CircularProgressIndicator());
            break;
          case ModelState.loadSuccess:
            return TfliteCamera(cameras: cameras);
            break;
          case ModelState.loadError:
            return Center(child: Text("Couldn't load the TFlite model."));
            break;
          default:
            return Center(child: Text("Something went wrong."));
        }
      }()),
    );
  }
}

/// Responsible for loading camera and passing frames to Tflite model for
/// analysis.
class TfliteCamera extends StatefulWidget {
  final List<CameraDescription> cameras;

  const TfliteCamera({Key key, @required this.cameras}) : super(key: key);

  @override
  _TfliteCameraState createState() => _TfliteCameraState();
}

class _TfliteCameraState extends State<TfliteCamera> {
  CameraController controller;
  List<dynamic> _recognitions;
  bool isDetecting = false;

  @override
  void setState(VoidCallback fn) {
    // check if still mounted before setting state
    if (mounted) {
      super.setState(fn);
    }
  }

  @override
  void initState() {
    super.initState();

    if (widget.cameras == null || widget.cameras.length < 1) {
      print('No camera is found');
    } else {
      // create and initialize camera controller, to get access to camera stream
      controller = CameraController(
        widget.cameras[0],
        ResolutionPreset.max,
        enableAudio: false, // this prevents app asking for audio permission
      );
      controller.initialize().then((_) {
        setState(() {});

        // image analysis with CNN starts here
        controller.startImageStream((CameraImage img) {
          if (!isDetecting) {
            Tflite.runModelOnFrame(
              bytesList: img.planes.map((plane) => plane.bytes).toList(),
              imageHeight: img.height,
              imageWidth: img.width,
              // by providing the values below for mean and SD, the img passed
              // to model is converted to the range [-1, 1]
              imageMean: 127.5,
              imageStd: 127.5,
              numResults: 1,
            ).then((recognitions) {
              setState(() => _recognitions = recognitions);
              print(recognitions);
              isDetecting = false;
            });
          }
        });
      });
    }
  }

  @override
  void dispose() {
    controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return controller.value.isInitialized
        ? Stack(children: [
            CameraPreview(controller),
            Align(
              alignment: Alignment.bottomCenter,
              child: Text(
                  "Prediction: ${_recognitions != null ? _recognitions.last['label'] : ''}"),
            ),
          ])
        : Center(child: CircularProgressIndicator());
  }
}

/// Enum to keep track of whether the CNN model has been loaded or not.
enum ModelState { loading, loadSuccess, loadError }
