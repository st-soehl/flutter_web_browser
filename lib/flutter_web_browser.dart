import 'dart:async';
import 'dart:ui';

import 'package:flutter/services.dart';

enum SafariViewControllerDismissButtonStyle {
  done,
  close,
  cancel,
}

class SafariViewControllerOptions {
  final bool barCollapsingEnabled;
  final bool entersReaderIfAvailable;
  final Color? preferredBarTintColor;
  final Color? preferredControlTintColor;
  final bool modalPresentationCapturesStatusBarAppearance;
  final SafariViewControllerDismissButtonStyle? dismissButtonStyle;

  const SafariViewControllerOptions({
    this.barCollapsingEnabled = false,
    this.entersReaderIfAvailable = false,
    this.preferredBarTintColor,
    this.preferredControlTintColor,
    this.modalPresentationCapturesStatusBarAppearance = false,
    this.dismissButtonStyle,
  });
}

enum CustomTabsColorScheme {
  system, // 0x00000000
  light, // 0x00000001
  dark, // 0x00000002
}

class CustomTabsOptions {
  final CustomTabsColorScheme colorScheme;
  final Color? toolbarColor;
  final Color? secondaryToolbarColor;
  final Color? navigationBarColor;
  final bool instantAppsEnabled;
  final bool addDefaultShareMenuItem;
  final bool showTitle;
  final bool urlBarHidingEnabled;

  const CustomTabsOptions({
    this.colorScheme = CustomTabsColorScheme.system,
    this.toolbarColor,
    this.secondaryToolbarColor,
    this.navigationBarColor,
    this.instantAppsEnabled = false,
    this.addDefaultShareMenuItem = false,
    this.showTitle = false,
    this.urlBarHidingEnabled = false,
  });
}

extension _hexColor on Color {
  /// Returns the color value as ARGB hex value.
  String get hexColor {
    return '#' + value.toRadixString(16).padLeft(8, '0');
  }
}

class FlutterWebBrowser {
  static const MethodChannel _channel =
      const MethodChannel('flutter_web_browser');

  static Future<bool> warmup() async {
    return await _channel.invokeMethod<bool>('warmup') ?? true;
  }

  
  
  static Future<OpenPageResult> openWebPage({
    required String url,
    CustomTabsOptions customTabsOptions = const CustomTabsOptions(),
    SafariViewControllerOptions safariVCOptions =
        const SafariViewControllerOptions(),
  }) {
    return _channel.invokeMethod('openWebPage', {
      "url": url,
      'android_options': {
        'colorScheme': customTabsOptions.colorScheme.index,
        'navigationBarColor': customTabsOptions.navigationBarColor?.hexColor,
        'toolbarColor': customTabsOptions.toolbarColor?.hexColor,
        'secondaryToolbarColor':
            customTabsOptions.secondaryToolbarColor?.hexColor,
        'instantAppsEnabled': customTabsOptions.instantAppsEnabled,
        'addDefaultShareMenuItem': customTabsOptions.addDefaultShareMenuItem,
        'showTitle': customTabsOptions.showTitle,
        'urlBarHidingEnabled': customTabsOptions.urlBarHidingEnabled,
      },
      'ios_options': {
        'barCollapsingEnabled': safariVCOptions.barCollapsingEnabled,
        'entersReaderIfAvailable': safariVCOptions.entersReaderIfAvailable,
        'preferredBarTintColor':
            safariVCOptions.preferredBarTintColor?.hexColor,
        'preferredControlTintColor':
            safariVCOptions.preferredControlTintColor?.hexColor,
        'modalPresentationCapturesStatusBarAppearance':
            safariVCOptions.modalPresentationCapturesStatusBarAppearance,
        'dismissButtonStyle': safariVCOptions.dismissButtonStyle?.index,
      },
    }).then((value) {
      if (value == 1){
        return OpenPageResult.opened_custom_tab;
      }else if(value == 0){
        return OpenPageResult.opened_via_default_browser;
      }else{
        return OpenPageResult.no_broswer_available;
      }
    });
  }
}

enum OpenPageResult{
  opened_custom_tab, opened_via_default_browser, no_broswer_available
}
