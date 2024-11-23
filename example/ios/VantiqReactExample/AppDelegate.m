#import "AppDelegate.h"
#import <UserNotifications/UserNotifications.h>
#import "VantiqUI.h"
#import "LastActive.h"
#import "VantiqReact.h"

#import <React/RCTBundleURLProvider.h>

// our globally-available Vantiq UI bridge variables
extern VantiqUI *vui;
extern NSString *APNSDeviceToken;

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  self.moduleName = @"VantiqReactExample";
  // You can add your custom initial props in the dictionary below.
  // They will be passed down to the ViewController used by React Native.
  self.initialProps = @{};
  
  // allow the user to select which type of notifications to receive, if any
  UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
  center.delegate = (id<UNUserNotificationCenterDelegate>)self;
  [center requestAuthorizationWithOptions:(UNAuthorizationOptionSound | UNAuthorizationOptionAlert | UNAuthorizationOptionBadge)
      completionHandler:^(BOOL granted, NSError * _Nullable error){
      if (error) {
          NSLog(@"Error in registering for notifications: %@", [error localizedDescription]);
      } else {
          dispatch_async(dispatch_get_main_queue(), ^ {
              // register for an APNS token
              [[UIApplication sharedApplication] registerForRemoteNotifications];
          });
      };
  }];

  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    // remember token for use in registering it with the Vantiq server
    APNSDeviceToken = [VantiqUI convertAPNSToken:deviceToken];
    NSLog(@"APNSDeviceToken = %@", [VantiqUI convertAPNSToken:deviceToken]);
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(nonnull NSError *)error {
    NSLog(@"Failed to receive token: %@", [error localizedDescription]);
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(nonnull NSDictionary *)userInfo
  fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))fetchCompletionHandler {
  NSLog(@"didReceiveRemoteNotification: userInfo (state = %ld) = %@", application.applicationState, userInfo);
  if (vui) {
      [vui processPushNotification:userInfo completionHandler:^(BOOL notificationHandled) {
          if (notificationHandled) {
              NSLog(@"didReceiveRemoteNotification: calling completion handler.");
              fetchCompletionHandler(UIBackgroundFetchResultNewData);;
          } else {
              // this notification must be handled (or not) by the app
              id notifyData = [userInfo objectForKey:@"data"];
              if (notifyData) {
                  NSString *dataType = [notifyData objectForKey:@"type"];
                  NSLog(@"didReceiveRemoteNotification: unhandled notification type = '%@'.", dataType);
                
                  // forward the notify data to the React Native app
                  // see https://github.com/facebook/react-native/issues/15421
                  VantiqReact *vr = [VantiqReact allocWithZone:nil];
                  [vr sendEventWithName:@"pushNotification" body:notifyData];
              }
              fetchCompletionHandler(UIBackgroundFetchResultNewData);
          }
      }];
  } else {
      // user hasn't logged in so nothing to do yet
      fetchCompletionHandler(UIBackgroundFetchResultNoData);
  }
}

- (void)application:(UIApplication *)application performFetchWithCompletionHandler:(void (^)(UIBackgroundFetchResult))fetchCompletionHandler {
    if (vui) {
        // do our background tasks and call the completion handler when we're finished
        [vui doBFTasksWithCompletionHandler:NO completionHandler:^(BOOL notificationHandled) {
            fetchCompletionHandler(UIBackgroundFetchResultNewData);
        }];
    }
}

- (void)applicationDidEnterBackground:(UIApplication *)application{
    [[LastActive sharedInstance] enterBackground];
}

- (void)applicationWillEnterForeground:(UIApplication *)application{
    [[LastActive sharedInstance] enterForeground];
}

- (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
{
  return [self bundleURL];
}

- (NSURL *)bundleURL
{
#if DEBUG
  return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index"];
#else
  return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
#endif
}

@end
