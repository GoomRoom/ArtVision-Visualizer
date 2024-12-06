import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebViewPage extends StatefulWidget {
  const WebViewPage({Key? key}) : super(key: key);

  @override
  State<WebViewPage> createState() => _WebViewPageState();
}

class _WebViewPageState extends State<WebViewPage> {
  late final WebViewController controller;

  @override
  void initState() {
    super.initState();

    controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageFinished: (String url) {
            // Inject JavaScript to remove the footer
            controller.runJavaScript(
                "var footer = document.querySelector('footer');"
                    "if (footer) { footer.style.display = 'none'; }"
            );
          },
          // Other callbacks can remain as they are
        ),
      )
      ..loadRequest(Uri.parse('https://artvision.artnbuff.com/'));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Interactive Space'),
      ),
      body: WebViewWidget(controller: controller),
    );
  }
}
