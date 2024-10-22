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
import 'dart:math';

void main() => runApp(ARFlutterApp());

class ARFlutterApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AR Flutter App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: ARHomePage(),
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
            planeDetectionConfig: PlaneDetectionConfig.horizontalAndVertical,
          ),
          Positioned(
            bottom: 20,
            left: 20,
            right: 20,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    IconButton(
                      icon: Icon(Icons.art_track),
                      onPressed: () => _swapArtwork(),
                      tooltip: 'Swap Artwork',
                    ),
                    IconButton(
                      icon: Icon(Icons.photo),
                      onPressed: () => _swapFrame(),
                      tooltip: 'Swap Frame',
                    ),
                  ],
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

  void onARViewCreated(ARSessionManager arSessionManager, ARObjectManager arObjectManager, ARAnchorManager arAnchorManager, ARLocationManager arLocationManager) {
    this.arSessionManager = arSessionManager;
    this.arObjectManager = arObjectManager;
    this.arAnchorManager = arAnchorManager;

    arSessionManager.onInitialize(
      showFeaturePoints: false,
      showPlanes: true,
      customPlaneTexturePath: "images/plane_texture.png",
      showWorldOrigin: true,
    );
    arObjectManager.onInitialize();

    arSessionManager.onPlaneOrPointTap = onPlaneOrPointTapped;
  }

  Future<void> _swapArtwork() async {
    if (artworkNode != null) {
      await arObjectManager.removeNode(artworkNode!);
    }

    artworkNode = ARNode(
      type: NodeType.localGLTF2,
      uri: "assets/artwork_plane.gltf",
      position: vector.Vector3(0.0, 0.0, -1.0),
      scale: vector.Vector3(1.0, 1.0, 1.0),
      rotation: vector.Vector4(1.0, 0.0, 0.0, 0.0),
    );

    arObjectManager.addNode(artworkNode!);
  }

  Future<void> _swapFrame() async {
    if (frameNode != null) {
      await arObjectManager.removeNode(frameNode!);
    }

    frameNode = ARNode(
      type: NodeType.localGLTF2,
      uri: "assets/frame1.gltf",
      position: vector.Vector3(0.0, 0.0, -1.0),
      scale: vector.Vector3(1.0, 1.0, 1.0),
      rotation: vector.Vector4(1.0, 0.0, 0.0, 0.0),
    );

    arObjectManager.addNode(frameNode!);
  }

  Future<void> onPlaneOrPointTapped(List<ARHitTestResult> hitTestResults) async {
    var singleHitTestResult = hitTestResults.firstWhere(
            (hitTestResult) => hitTestResult.type == ARHitTestResultType.plane);
    if (singleHitTestResult != null) {
      vector.Vector3 position = vector.Vector3(
        singleHitTestResult.worldTransform.getTranslation().x,
        singleHitTestResult.worldTransform.getTranslation().y,
        singleHitTestResult.worldTransform.getTranslation().z,
      );

      var newAnchor = ARPlaneAnchor(transformation: singleHitTestResult.worldTransform);
      bool? didAddAnchor = await this.arAnchorManager.addAnchor(newAnchor);
      if (didAddAnchor!) {
        this.anchors.add(newAnchor);

        // Add artwork node to anchor
        if (artworkNode == null) {
          artworkNode = ARNode(
            type: NodeType.localGLTF2,
            uri: "assets/artwork_plane.gltf",
            scale: vector.Vector3(1.0, 1.0, 1.0),
            position: position,
            rotation: vector.Vector4(1.0, 0.0, 0.0, 0.0),
          );
          bool? didAddArtworkNode =
          await this.arObjectManager.addNode(artworkNode!, planeAnchor: newAnchor);
          if (didAddArtworkNode!) {
            this.nodes.add(artworkNode!);
          } else {
            this.arSessionManager.onError("Adding Artwork Node to Anchor failed");
          }
        }

        // Add frame node to anchor
        if (frameNode == null) {
          frameNode = ARNode(
            type: NodeType.localGLTF2,
            uri: "assets/frame1.gltf",
            scale: vector.Vector3(1.0, 1.0, 1.0),
            position: position,
            rotation: vector.Vector4(1.0, 0.0, 0.0, 0.0),
          );
          bool? didAddFrameNode =
          await this.arObjectManager.addNode(frameNode!, planeAnchor: newAnchor);
          if (didAddFrameNode!) {
            this.nodes.add(frameNode!);
          } else {
            this.arSessionManager.onError("Adding Frame Node to Anchor failed");
          }
        }
      } else {
        this.arSessionManager.onError("Adding Anchor failed");
      }
    }
  }

  Future<void> _removeEverything() async {
    anchors.forEach((anchor) {
      this.arAnchorManager.removeAnchor(anchor);
    });
    anchors = [];
    nodes = [];
    frameNode = null;
    artworkNode = null;
  }
}
