import 'package:flutter/material.dart';
import 'package:carousel_slider/carousel_slider.dart';
import 'dart:io';
import 'package:shared_preferences/shared_preferences.dart';
import 'gallery_page.dart';
import 'artwork_initializer.dart';
import 'package:photo_view/photo_view.dart';
import 'package:url_launcher/url_launcher.dart'; // Import url_launcher

class HomePage extends StatefulWidget {
  final Function(int) setIndex;

  const HomePage({super.key, required this.setIndex});

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with SingleTickerProviderStateMixin {
  final List<String> backgroundImages = [
    'assets/1.jpg',
    'assets/2.jpg',
    'assets/3.jpg',
    'assets/4.jpg',
    'assets/5.jpg',
    'assets/6.jpg',
    'assets/7.jpg',
    'assets/8.jpg',
    'assets/9.jpg',
    'assets/10.jpg',
    'assets/11.jpg',
    'assets/12.jpg',
    'assets/13.jpg',
  ];

  final List<Offset> defaultArtworkPositions = [
    const Offset(0.12, 0.12),
    const Offset(0.12, 0.12),
    const Offset(0.12, 0.12),
    const Offset(0.12, 0.15),
    const Offset(0.12, 0.08),
    const Offset(0.12, 0.15),
    const Offset(0.12, 0.2),
    const Offset(0.12, 0.2),
    const Offset(0.12, 0.12),
    const Offset(0.12, 0.2),
    const Offset(0.12, 0.2),
    const Offset(0.12, 0.2),
    const Offset(0.12, 0.2),
  ];

  static const double artworkSize = 300.0;
  int _currentBackgroundIndex = 0;
  int _selectedArtworkIndex = -1;
  String _selectedTab = "artworks";
  late TabController _tabController;

  // Variables for border customization
  bool _borderEnabled = false;
  Color _borderColor = Colors.black;
  double _borderThickness = 2.0;

  // Dynamic list to store artwork image paths
  List<String> artworkImages = [];

  @override
  void initState() {
    super.initState();
    _loadSavedArtworks(); // Load artworks from SharedPreferences
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(_handleTabSelection);
  }

  Future<void> _loadSavedArtworks() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    List<String>? artworks = prefs.getStringList('savedArtworks');

    if (artworks == null || artworks.isEmpty) {
      // If savedArtworks is empty, initialize with demo artworks
      artworks = await ArtworkInitializer.initializeDemoArtworks();
      prefs.setStringList('savedArtworks', artworks);
    }

    setState(() {
      artworkImages = artworks!;
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _handleTabSelection() {
    setState(() {
      if (_tabController.index == 0) {
        _selectedTab = "artworks";
      } else if (_tabController.index == 1) {
        _selectedTab = "frames"; // 'frames' is now associated with border options
      } else if (_tabController.index == 2) {
        _selectedTab = "ratios";
      }
    });
  }

  Widget _buildMenu1() {
    return Positioned(
      bottom: 100,
      left: 20,
      right: 20,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(25.0),
          boxShadow: const [
            BoxShadow(
              color: Colors.black12,
              blurRadius: 10.0,
              offset: Offset(0, 5),
            ),
          ],
        ),
        child: TabBar(
          controller: _tabController,
          indicator: BoxDecoration(
            color: Colors.cyan.shade200,
            borderRadius: BorderRadius.circular(25.0),
          ),
          labelColor: Colors.white,
          unselectedLabelColor: Colors.black54,
          indicatorSize: TabBarIndicatorSize.tab,
          tabs: const [
            Tab(
              text: 'Artwork',
            ),
            Tab(
              text: 'Border',
            ),
            // Uncomment if implementing aspect ratios
            // Tab(
            //   text: 'Aspect Ratio',
            // ),
          ],
        ),
      ),
    );
  }

  Widget _buildMenu2() {
    if (_selectedTab == "artworks") {
      return _buildArtworkMenu();
    } else if (_selectedTab == "frames") {
      return _buildBorderMenu();
    } else if (_selectedTab == "ratios") {
      return _buildRatioMenu();
    }
    return const SizedBox.shrink();
  }

  Widget _buildArtworkMenu() {
    return Positioned(
      bottom: 0,
      left: 0,
      right: 0,
      child: SizedBox(
        height: 80,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Expanded(
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: List.generate(
                    artworkImages.length,
                        (index) {
                      return GestureDetector(
                        onTap: () {
                          setState(() {
                            if (_selectedArtworkIndex == index) {
                              _selectedArtworkIndex = -1;
                            } else {
                              _selectedArtworkIndex = index;
                            }
                          });
                        },
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 4.0),
                          child: Container(
                            width: 60,
                            height: 60,
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(15.0),
                              boxShadow: const [
                                BoxShadow(
                                  color: Colors.black12,
                                  blurRadius: 5.0,
                                  offset: Offset(0, 3),
                                ),
                              ],
                            ),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(15.0),
                              child: Image.file(
                                File(artworkImages[index]),
                                fit: BoxFit.cover,
                              ),
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4.0),
              child: Container(
                width: 60,
                height: 60,
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(15.0),
                  boxShadow: const [
                    BoxShadow(
                      color: Colors.black12,
                      blurRadius: 5.0,
                      offset: Offset(0, 3),
                    ),
                  ],
                ),
                child: IconButton(
                  icon: const Icon(Icons.add, size: 24, color: Colors.blue),
                  onPressed: () async {
                    // Navigate to the GalleryPage
                    await Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const GalleryPage()),
                    );
                    _loadSavedArtworks(); // Reload artworks after returning from gallery
                  },
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBorderMenu() {
    return Positioned(
      bottom: 0,
      left: 0,
      right: 0,
      child: Container(
        height: 100, // Adjusted height
        color: Colors.white.withOpacity(0.9),
        child: Column(
          children: [
            SwitchListTile(
              dense: true,
              contentPadding: const EdgeInsets.symmetric(horizontal: 16.0),
              title: const Text(
                'Enable Border',
                style: TextStyle(fontSize: 14.0),
              ),
              value: _borderEnabled,
              onChanged: (value) {
                setState(() {
                  _borderEnabled = value;
                  if (!_borderEnabled) {
                    _borderThickness = 0.0;
                  } else {
                    if (_borderThickness == 0.0) {
                      _borderThickness = 2.0; // default thickness
                    }
                  }
                });
              },
            ),
            if (_borderEnabled)
              Expanded(
                child: Row(
                  children: [
                    // Color Picker
                    Expanded(
                      child: SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: _availableBorderColors.map((color) {
                            return GestureDetector(
                              onTap: () {
                                setState(() {
                                  _borderColor = color;
                                });
                              },
                              child: Container(
                                margin: const EdgeInsets.symmetric(horizontal: 5.0),
                                width: 30,
                                height: 30,
                                decoration: BoxDecoration(
                                  color: color,
                                  shape: BoxShape.circle,
                                  border: _borderColor == color
                                      ? Border.all(color: Colors.black, width: 2)
                                      : null,
                                ),
                              ),
                            );
                          }).toList(),
                        ),
                      ),
                    ),
                    // Thickness Slider
                    SizedBox(
                      width: 150,
                      child: Row(
                        children: [
                          const Text(
                            'Thickness',
                            style: TextStyle(fontSize: 12.0),
                          ),
                          Expanded(
                            child: Slider(
                              value: _borderThickness,
                              min: 1.0,
                              max: 10.0,
                              onChanged: (value) {
                                setState(() {
                                  _borderThickness = value;
                                });
                              },
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  // List of available border colors
  final List<Color> _availableBorderColors = [
    Colors.black,
    Colors.white,
    Colors.red,
    Colors.green,
    Colors.blue,
    Colors.yellow,
    Colors.orange,
    Colors.purple,
    Colors.brown,
    Colors.grey,
  ];

  Widget _buildRatioMenu() {
    // Placeholder for aspect ratio menu
    return const SizedBox.shrink();
  }

  @override
  Widget build(BuildContext context) {
    // Adjust artwork size based on aspect ratio if needed
    double artworkWidth = artworkSize;
    double artworkHeight = artworkSize;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Home Page'),
        actions: [
          IconButton(
            icon: const Icon(Icons.info_outline),
            onPressed: () {
              _showInfoDialog(context); // Show the info dialog
            },
          ),
        ],
      ),
      body: Stack(
        children: [
          // Background Carousel
          CarouselSlider(
            options: CarouselOptions(
              height: MediaQuery.of(context).size.height,
              viewportFraction: 1.0,
              onPageChanged: (index, reason) {
                setState(() {
                  _currentBackgroundIndex = index;
                });
              },
            ),
            items: backgroundImages.map((imagePath) {
              return Container(
                decoration: BoxDecoration(
                  image: DecorationImage(
                    image: AssetImage(imagePath),
                    fit: BoxFit.fitHeight,
                  ),
                ),
              );
            }).toList(),
          ),
          // Display selected artwork
          if (_selectedArtworkIndex != -1)
            Positioned(
              top: MediaQuery.of(context).size.height *
                  defaultArtworkPositions[_currentBackgroundIndex].dy,
              left: MediaQuery.of(context).size.width *
                  defaultArtworkPositions[_currentBackgroundIndex].dx,
              child: Container(
                width: artworkWidth,
                height: artworkHeight,
                decoration: BoxDecoration(
                  border: _borderEnabled
                      ? Border.all(color: _borderColor, width: _borderThickness)
                      : null,
                ),
                child: Image.file(
                  File(artworkImages[_selectedArtworkIndex]),
                  fit: BoxFit.contain,
                ),
              ),
            ),
          _buildMenu1(), // TabBar menu
          _buildMenu2(), // Corresponding menu content
        ],
      ),
    );
  }

  void _showInfoDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('App Information'),
          content: SingleChildScrollView(
            child: ListBody(
              children: [
                const Text('We believe that art has the power to unite the world together. We aim to build a dynamic and passionate community of individuals who share our values of embracing diverse artistic expressions. We can make a significant impact by promoting global cultural appreciation and empowering talented artists to showcase their skills. Keep reading to learn more about Artnbuff.'), // Replace with your app's summary
                const SizedBox(height: 20),
                TextButton(
                  onPressed: () {
                    _launchURL('https://galleria.artnbuff.com/general-terms-conditions/');
                  },
                  child: const Text('Terms and Conditions'),
                ),
                TextButton(
                  onPressed: () {
                    _launchURL('https://galleria.artnbuff.com/privacy-policy/');
                  },
                  child: const Text('Privacy Policy'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close the dialog
              },
              child: const Text('Close'),
            ),
          ],
        );
      },
    );
  }

  void _launchURL(String url) async {
    final Uri uri = Uri.parse(url);
    if (!await launchUrl(uri)) {
      throw 'Could not launch $url';
    }
  }
}
