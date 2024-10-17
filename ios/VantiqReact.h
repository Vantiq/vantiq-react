
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNVantiqReactSpec.h"

@interface VantiqReact : NSObject <NativeVantiqReactSpec>
#else
#import <React/RCTBridgeModule.h>

@interface VantiqReact : NSObject <RCTBridgeModule>
#endif

@end
