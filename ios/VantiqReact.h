
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNVantiqReactSpec.h"

@interface VantiqReact : NSObject <NativeVantiqReactSpec>
#else
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface VantiqReact : RCTEventEmitter <RCTBridgeModule>
#endif

@end
