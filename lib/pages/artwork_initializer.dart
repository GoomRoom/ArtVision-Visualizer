import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/services.dart' show ByteData, rootBundle;

class ArtworkInitializer {
  static Future<List<String>> initializeDemoArtworks() async {
    List<String> demoArtworks = [];

    // List of demo asset paths
    List<String> demoAssetPaths = [
      'assets/artwork1.png',
      'assets/artwork2.png',
      'assets/artwork3.png',
    ];

    final directory = await getApplicationDocumentsDirectory();

    for (String assetPath in demoAssetPaths) {
      // Read the asset image as bytes
      ByteData data = await rootBundle.load(assetPath);
      List<int> bytes = data.buffer.asUint8List();

      // Create a unique file name
      String fileName = assetPath.split('/').last;
      File file = File('${directory.path}/$fileName');

      // Write the bytes to a file
      await file.writeAsBytes(bytes);

      // Add the file path to the list
      demoArtworks.add(file.path);
    }

    return demoArtworks;
  }
}
