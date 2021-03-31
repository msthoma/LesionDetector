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
  void initState() {
    super.initState();
    initModel();
  }

  void initModel() async {
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

  @override
  void initState() {
    super.initState();
    controller = CameraController(
      widget.cameras[0],
      ResolutionPreset.max,
      enableAudio: false,
    );
    controller.initialize().then((_) {
      if (!mounted) {
        return;
      }
      setState(() {});
    });
  }

  @override
  void dispose() {
    controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return controller.value.isInitialized
        ? CameraPreview(controller)
        : Center(child: CircularProgressIndicator());
  }
}

/// Enum to keep track of whether the CNN model has been loaded or not.
enum ModelState { loading, loadSuccess, loadError }
