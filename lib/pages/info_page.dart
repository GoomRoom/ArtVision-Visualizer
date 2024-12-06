import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class MyHomePage extends StatelessWidget {
  // Replace with your actual tab count
  final int _tabCount = 3;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: _tabCount,
      child: Scaffold(
        appBar: AppBar(
          title: Text('Your App Title'),
          bottom: TabBar(
            tabs: [
              Tab(text: 'Tab 1'),
              Tab(text: 'Tab 2'),
              Tab(text: 'Tab 3'),
            ],
          ),
          actions: [
            IconButton(
              icon: Icon(Icons.info_outline),
              onPressed: () {
                _showInfoDialog(context);
              },
            ),
          ],
        ),
        body: TabBarView(
          children: [
            // Your tab views go here
            Center(child: Text('Tab 1 Content')),
            Center(child: Text('Tab 2 Content')),
            Center(child: Text('Tab 3 Content')),
          ],
        ),
      ),
    );
  }

  void _showInfoDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('App Information'),
          content: SingleChildScrollView(
            child: ListBody(
              children: [
                Text('This app allows you to...'),
                SizedBox(height: 20),
                TextButton(
                  onPressed: () {
                    _launchURL('https://yourwebsite.com/terms');
                  },
                  child: Text('Terms and Conditions'),
                ),
                TextButton(
                  onPressed: () {
                    _launchURL('https://yourwebsite.com/privacy');
                  },
                  child: Text('Privacy Policy'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close the dialog
              },
              child: Text('Close'),
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
