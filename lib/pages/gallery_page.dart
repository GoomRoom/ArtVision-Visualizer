import 'package:flutter/material.dart';
import 'dart:io';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'artwork_initializer.dart'; // Import the utility class

class GalleryPage extends StatefulWidget {
  const GalleryPage({Key? key}) : super(key: key);

  @override
  _GalleryPageState createState() => _GalleryPageState();
}

class _GalleryPageState extends State<GalleryPage> {
  List<String> savedArtworks = [];

  @override
  void initState() {
    super.initState();
    _loadSavedArtworks();
  }

  Future<void> _loadSavedArtworks() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    List<String>? artworks = prefs.getStringList('savedArtworks');

    if (artworks == null || artworks.isEmpty) {
      // Initialize demo artworks
      artworks = await ArtworkInitializer.initializeDemoArtworks();
      prefs.setStringList('savedArtworks', artworks);
    }

    setState(() {
      savedArtworks = artworks!;
    });
  }

  Future<void> _pickImage() async {
    final ImagePicker picker = ImagePicker();
    final XFile? image =
    await picker.pickImage(source: ImageSource.gallery, imageQuality: 85);

    if (image != null) {
      final directory = await getApplicationDocumentsDirectory();
      final String path = directory.path;
      final String fileName = DateTime.now().millisecondsSinceEpoch.toString();
      final File newImage = await File(image.path).copy('$path/$fileName.png');

      setState(() {
        savedArtworks.add(newImage.path);
      });

      SharedPreferences prefs = await SharedPreferences.getInstance();
      prefs.setStringList('savedArtworks', savedArtworks);
    }
  }

  Future<void> _deleteImage(int index) async {
    final File file = File(savedArtworks[index]);
    if (await file.exists()) {
      await file.delete();
    }

    setState(() {
      savedArtworks.removeAt(index);
    });

    SharedPreferences prefs = await SharedPreferences.getInstance();
    prefs.setStringList('savedArtworks', savedArtworks);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Gallery Page'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _pickImage,
          ),
        ],
      ),
      body: savedArtworks.isEmpty
          ? const Center(child: Text('No images in the gallery'))
          : GridView.builder(
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 3,
        ),
        itemCount: savedArtworks.length,
        itemBuilder: (context, index) {
          return GestureDetector(
            onLongPress: () => _deleteImage(index),
            child: Image.file(File(savedArtworks[index])),
          );
        },
      ),
    );
  }
}
