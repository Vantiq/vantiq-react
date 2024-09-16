
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNVantiqInterfaceLibrarySpec.h"

@interface VantiqInterfaceLibrary : NSObject <NativeVantiqInterfaceLibrarySpec>
#else
#import <React/RCTBridgeModule.h>

@interface VantiqInterfaceLibrary : NSObject <RCTBridgeModule>
#endif

@end
