import 'package:ar_flutter_plugin/datatypes/config_planedetection.dart';
import 'package:ar_flutter_plugin/datatypes/hittest_result_types.dart';
import 'package:ar_flutter_plugin/datatypes/node_types.dart';
import 'package:ar_flutter_plugin/managers/ar_anchor_manager.dart';
import 'package:ar_flutter_plugin/managers/ar_location_manager.dart';
import 'package:ar_flutter_plugin/managers/ar_object_manager.dart';
import 'package:ar_flutter_plugin/managers/ar_session_manager.dart';
import 'package:ar_flutter_plugin/models/ar_anchor.dart';
import 'package:ar_flutter_plugin/models/ar_node.dart';
import 'package:ar_flutter_plugin/models/ar_hittest_result.dart';
import 'package:flutter/material.dart';
import 'package:ar_flutter_plugin/ar_flutter_plugin.dart';
import 'package:vector_math/vector_math_64.dart' as vector;

void main() => runApp(ARFlutterApp());

class ARFlutterApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AR Flutter App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: StartScreen(),
    );
  }
}

class StartScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: ElevatedButton(
          onPressed: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => ARHomePage()),
          ),
          child: Text('Tap to Begin AR Session'),
        ),
      ),
    );
  }
}

class ARHomePage extends StatefulWidget {
  @override
  _ARHomePageState createState() => _ARHomePageState();
}

class _ARHomePageState extends State<ARHomePage> {
  late ARSessionManager arSessionManager;
  late ARObjectManager arObjectManager;
  late ARAnchorManager arAnchorManager;
  List<ARNode> nodes = [];
  List<ARAnchor> anchors = [];
  ARNode? frameNode;
  ARNode? artworkNode;
  ARAnchor? currentAnchor;
  String selectedArtwork = 'assets/artwork_plane1.gltf';
  String selectedFrame = 'assets/frame1.gltf';

  @override
  void dispose() {
    super.dispose();
    arSessionManager.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("AR Artwork Display"),
      ),
      body: Stack(
        children: [
          ARView(
            onARViewCreated: onARViewCreated,
            planeDetectionConfig: PlaneDetectionConfig.vertical,
          ),
          Positioned(
            bottom: 20,
            left: 20,
            right: 20,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DefaultTabController(
                  length: 2,
                  child: Column(
                    children: [
                      TabBar(
                        tabs: [
                          Tab(text: 'Artworks'),
                          Tab(text: 'Frames'),
                        ],
                      ),
                      Container(
                        height: 100,
                        child: TabBarView(
                          children: [
                            _buildArtworkSelection(),
                            _buildFrameSelection(),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                ElevatedButton(
                  onPressed: _removeEverything,
                  child: Text("Remove Everything"),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }

  Widget _buildArtworkSelection() {
    List<String> artworkOptions = [
      'assets/artwork_plane1.gltf',
      'assets/artwork_plane2.gltf',
      'assets/artwork_plane3.gltf',
    ];

    List<String> artworkPreviewImages = [
      'images/artwork_preview1.png',
      'images/artwork_preview2.png',
      'images/artwork_preview3.png',
    ];

    return ListView.builder(
      scrollDirection: Axis.horizontal,
      itemCount: artworkOptions.length,
      itemBuilder: (context, index) {
        String artwork = artworkOptions[index];
        String previewImage = artworkPreviewImages[index];
        return GestureDetector(
          onTap: () {
            setState(() {
              selectedArtwork = artwork;
            });
          },
          child: Container(
            width: 80,
            height: 70,
            margin: EdgeInsets.symmetric(horizontal: 10),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16), // Rounded edges
              border: Border.all(
                color: selectedArtwork == artwork ? Colors.blue : Colors.grey,
                width: 3,
              ),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16), // Rounded edges for the image
              child: Image.asset(
                previewImage,
                width: 70,
                height: 70,
                fit: BoxFit.cover,
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildFrameSelection() {
    List<String> frameOptions = [
      'assets/frame1.gltf',
      'assets/frame2.gltf',
      'assets/frame3.gltf',
    ];

    List<String> framePreviewImages = [
      'images/frame_preview1.png',
      'images/frame_preview2.png',
      'images/frame_preview3.png',
    ];

    return ListView.builder(
      scrollDirection: Axis.horizontal,
      itemCount: frameOptions.length,
      itemBuilder: (context, index) {
        String frame = frameOptions[index];
        String previewImage = framePreviewImages[index];
        return GestureDetector(
          onTap: () {
            setState(() {
              selectedFrame = frame;
            });
          },
          child: Container(
            width: 100,
            height: 70,
            margin: EdgeInsets.symmetric(horizontal: 10),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16), // Rounded edges
              border: Border.all(
                color: selectedFrame == frame ? Colors.blue : Colors.grey,
                width: 3,
              ),
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16), // Rounded edges for the image
              child: Image.asset(
                previewImage,
                width: 70,
                height: 70,
                fit: BoxFit.cover,
              ),
            ),
          ),
        );
      },
    );
  }


  void onARViewCreated(ARSessionManager arSessionManager, ARObjectManager arObjectManager, ARAnchorManager arAnchorManager, ARLocationManager arLocationManager) {
    this.arSessionManager = arSessionManager;
    this.arObjectManager = arObjectManager;
    this.arAnchorManager = arAnchorManager;

    arSessionManager.onInitialize(
      showFeaturePoints: true,
      showPlanes: true,
      customPlaneTexturePath: "images/triangle.png",
      showWorldOrigin: true,
    );
    arObjectManager.onInitialize();

    arSessionManager.onPlaneOrPointTap = onPlaneOrPointTapped;
  }

  Future<void> _addNodeToAnchor(vector.Matrix4 transformation) async {
    // Create anchor at the tapped position
    var newAnchor = ARPlaneAnchor(transformation: transformation);
    bool? didAddAnchor = await arAnchorManager.addAnchor(newAnchor);
    if (didAddAnchor!) {
      anchors.add(newAnchor);
      currentAnchor = newAnchor;

      // Add artwork node to anchor
      artworkNode = ARNode(
        type: NodeType.localGLTF2,
        uri: selectedArtwork,
        scale: vector.Vector3(0.2, 0.2, 0.2),
        position: vector.Vector3(0.0, 0.0, 0.0), // Positioned relative to the anchor
        rotation: vector.Vector4(0.25, -0.25, 0.0, 0),
      );
      bool? didAddArtworkNode = await arObjectManager.addNode(artworkNode!, planeAnchor: newAnchor);
      if (!didAddArtworkNode!) {
        arSessionManager.onError("Adding Artwork Node to Anchor failed");
      }

      // Add frame node to anchor
      frameNode = ARNode(
        type: NodeType.localGLTF2,
        uri: selectedFrame,
        scale: vector.Vector3(0.2, 0.2, 0.2),
        position: vector.Vector3(0.0, 0.0, 0.0), // Positioned relative to the anchor
        rotation: vector.Vector4(0.25, -0.25, 0.0, 0),
      );
      bool? didAddFrameNode = await arObjectManager.addNode(frameNode!, planeAnchor: newAnchor);
      if (!didAddFrameNode!) {
        arSessionManager.onError("Adding Frame Node to Anchor failed");
      }
    } else {
      arSessionManager.onError("Adding Anchor failed");
    }
  }

  Future<void> onPlaneOrPointTapped(List<ARHitTestResult> hitTestResults) async {
    // Find the furthest hit test result
    ARHitTestResult? furthestHitTestResult;
    for (var hitTestResult in hitTestResults) {
      if (hitTestResult.type == ARHitTestResultType.plane) {
        if (furthestHitTestResult == null ||
            hitTestResult.distance > furthestHitTestResult.distance) {
          furthestHitTestResult = hitTestResult;
        }
      }
    }

    if (furthestHitTestResult != null) {
      // Add nodes at the location of the hit
      //vector.Matrix3 worldRotation = vector.Matrix3.identity();
      furthestHitTestResult.worldTransform.setRotationX(0);
      furthestHitTestResult.worldTransform.setRotationY(0);
      furthestHitTestResult.worldTransform.setRotationZ(0);
      await _addNodeToAnchor(furthestHitTestResult.worldTransform);
    }
  }

  Future<void> _removeEverything() async {
    for (var anchor in anchors) {
      await arAnchorManager.removeAnchor(anchor);
    }
    anchors = [];
    frameNode = null;
    artworkNode = null;
  }
}
