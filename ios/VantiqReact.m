#import "VantiqReact.h"
#import "VantiqUI.h"
#import <React/RCTEventEmitter.h>

/* error codes returned by rejected promises */
/* veNotAuthorized: returned when a REST-related call fails because of an expired token that cannot be refreshed. This is to be expected and should be followed by a call to either authWithOAuth or authWithInternal. */
NSString *veNotAuthorized = @"com.vantiq.notAuthorized";
/* veRESTError: returned when a REST-related call fails because of a Vantiq API error. This is a developer error of some sort because of, for example, a malformed request or a missing referenced Type or Procedure. */
NSString *veRESTError = @"com.vantiq.RESTError";
/* veOSError: returned when a REST-related call fails because of a network infrastructure error. This should be an uncommon error. */
NSString *veOSError = @"com.vantiq.OSError";
/* veServerType: returned when the Vantiq server type (Internal or OAuth) cannot be determined. This indicates a network error or a bad server URL passed to the init method. */
NSString *veServerType = @"com.vantiq.serverTypeUnknown";
/* veInvalidAuthToken: returned by the init method when a saved access token has expired and cannot be refreshed. This is to be expected and should be followed by a call to either authWithOAuth or authWithInternal. */
NSString *veInvalidAuthToken = @"com.vantiq.invalidAuthToken";
NSString *veJsonParseError = @"com.vantiq.JsonParseError";

@implementation VantiqReact {
    NSArray<NSString *> *supportedEvents;
}
RCT_EXPORT_MODULE()

// our globally-available Vantiq UI bridge variables
VantiqUI *vui;
NSString *APNSDeviceToken;

+ (id)allocWithZone:(struct _NSZone *)zone {
    static VantiqReact *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [super allocWithZone:zone];
    });
    return sharedInstance;
}

/*** Exported methods for Vantiq authentication-oriented operations
 **/
RCT_EXPORT_METHOD(init:(NSString *)serverURL namespace:(NSString *)namespace resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  vui = [[VantiqUI alloc] init:serverURL namespace:namespace completionHandler:^(NSDictionary *response) {
    dispatch_async(dispatch_get_main_queue(), ^ {
      // it's up to the caller to pick through the returned object to figure out the state
      resolve(response);
    });
  }];
}

RCT_EXPORT_METHOD(serverType:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [vui serverType:^(NSDictionary *response) {
    dispatch_async(dispatch_get_main_queue(), ^ {
      NSString *errorStr = [response objectForKey:@"errorStr"];
      if (!errorStr.length) {
        resolve(response);
      } else {
        reject(veServerType, errorStr, [self buildNSError:response]);
      }
    });
  }];
}

RCT_EXPORT_METHOD(verifyAuthToken:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [vui verifyAuthToken:^(NSDictionary *response) {
    dispatch_async(dispatch_get_main_queue(), ^ {
      NSString *errorStr = [response objectForKey:@"errorStr"];
      if (!errorStr.length) {
        resolve(response);
      } else {
        reject(veInvalidAuthToken, errorStr, [self buildNSError:response]);
      }
    });
  }];
}

RCT_EXPORT_METHOD(authWithOAuth:(NSString *)urlScheme clientId:(NSString *)clientId
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [vui authWithOAuth:urlScheme clientId:clientId completionHandler:^(NSDictionary *response) {
    dispatch_async(dispatch_get_main_queue(), ^ {
      NSString *errorStr = [response objectForKey:@"errorStr"];
      if (!errorStr.length) {
        resolve(response);
      } else {
        reject(veNotAuthorized, errorStr, [self buildNSError:response]);
      }
    });
  }];
}

RCT_EXPORT_METHOD(authWithInternal:(NSString *)username password:(NSString *)password
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [vui authWithInternal:username password:password completionHandler:^(NSDictionary *response) {
    dispatch_async(dispatch_get_main_queue(), ^ {
      NSString *errorStr = [response objectForKey:@"errorStr"];
      if (!errorStr.length) {
        resolve(response);
      } else {
        reject(veNotAuthorized, errorStr, [self buildNSError:response]);
      }
    });
  }];
}

RCT_EXPORT_METHOD(logout:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
    [vui logout:^(NSDictionary * _Nullable response) {
        dispatch_async(dispatch_get_main_queue(), ^ {
          NSString *errorStr = [response objectForKey:@"errorStr"];
          if (!errorStr.length) {
            resolve(response);
          } else {
            reject(veNotAuthorized, errorStr, [self buildNSError:response]);
          }
        });
    }];
}

// helper to build NSError instance for reject() calls
- (NSError *)buildNSError:(NSDictionary *)responseDict {
  // use the special NSLocalizedDescriptionKey for overriding the localizedDescription property
  NSDictionary *userInfo = [[NSDictionary alloc] initWithObjectsAndKeys:[responseDict objectForKey:@"errorStr"], NSLocalizedDescriptionKey, nil];
  // handle 401 errors specially
  NSNumber *rawCode = [responseDict objectForKey:@"statusCode"];
  NSInteger code = (rawCode && ([rawCode longValue] == 401)) ? NSURLErrorUserAuthenticationRequired : 0;
  NSError *err = [[NSError alloc] initWithDomain:NSURLErrorDomain code:code userInfo:userInfo];
  return err;
}

// helper to create the resolve/reject promise returns for each REST method
- (void)resolveRESTPromise:(id)data response:(NSHTTPURLResponse *)response error:(NSError *)error
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject {
  // always send back the promise returns on the main thread since there might be UI calls by the caller
  dispatch_async(dispatch_get_main_queue(), ^ {
    // form a possible error string based on the HTTP response or the error condition
    NSString *resultStr = @"";
    [vui formError:response error:error resultStr:&resultStr];
    if (!resultStr.length) {
      // no error so just return any data for the successful promise
      resolve(data);
    } else {
      // an error condition exists so form a dictionary from which to build an NSError
      // object for the failure promise
      NSDictionary *responseDict = [[NSDictionary alloc] initWithObjectsAndKeys:resultStr,
        @"errorStr", [NSNumber numberWithInteger:response.statusCode], @"statusCode", nil];
      // the error code depends on whether there was a network/REST API error or some kind of lower-level error
      reject(error ? veOSError : veRESTError, resultStr, [self buildNSError:responseDict]);
    }
  });
}

- (NSString *)dictionaryToJSONString:(id)dict {
  const char *className = object_getClassName(dict);
  return !strcmp(className, "__NSDictionaryM") || !strcmp(className, "__NSArrayM") ? [vui dictionaryToJSONString:dict] : nil;
}

// helper to send back authentication error failure promises
- (void)sendAuthReject:(RCTPromiseRejectBlock)reject {
  NSDictionary *responseDict = [[NSDictionary alloc] initWithObjectsAndKeys:@"Authorization fails", @"errorStr", nil];
  reject(veNotAuthorized, @"Authorization fails", [self buildNSError:responseDict]);
}

- (void)sendJSONReject:(RCTPromiseRejectBlock)reject {
  NSDictionary *responseDict = [[NSDictionary alloc] initWithObjectsAndKeys:@"Object parameter not valid", @"errorStr", nil];
  reject(veJsonParseError, @"Object parameter not valid", [self buildNSError:responseDict]);
}

/*** Exported methods for each of the Vantiq REST APIs
 **/
RCT_EXPORT_METHOD(select:(NSString *)type props:(NSArray *)props where:(NSDictionary *)where
  sort:(NSDictionary *)sort limit:(int)limit resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *whereStr = [self dictionaryToJSONString:where];
  whereStr = whereStr ? whereStr : @"{}";
  NSString *sortStr = [self dictionaryToJSONString:sort];
  sortStr = sortStr ? sortStr : @"{}";
  if (whereStr && sortStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v select:type props:props where:whereStr sort:sortStr limit:limit
    completionHandler:^(NSArray *data, NSHTTPURLResponse *response, NSError *error) {
          data = data ? data : @[];
          [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(selectOne:(NSString *)type id:(NSString *)id
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [vui ensureValidToken:^(NSDictionary *response) {
    BOOL authValid = [response objectForKey:@"authValid"];
    if (authValid) {
      [vui.v selectOne:type id:id completionHandler:^(NSArray *data, NSHTTPURLResponse *response, NSError *error) {
        data = data ? data : @[];
        [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
      }];
    } else {
      [self sendAuthReject:reject];
    }
  }];
}

RCT_EXPORT_METHOD(insert:(NSString *)type object:(NSDictionary *)object
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *objectStr = [self dictionaryToJSONString:object];
  objectStr = objectStr ? objectStr : @"{}";
  if (objectStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v insert:type object:objectStr completionHandler:^(NSDictionary *data, NSHTTPURLResponse *response, NSError *error) {
          data = data ? data : [[NSDictionary alloc] init];
          [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(upsert:(NSString *)type object:(NSDictionary *)object
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *objectStr = [self dictionaryToJSONString:object];
  objectStr = objectStr ? objectStr : @"{}";
  if (objectStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v upsert:type object:objectStr completionHandler:^(NSDictionary *data, NSHTTPURLResponse *response, NSError *error) {
          data = data ? data : [[NSDictionary alloc] init];
          [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(count:(NSString *)type where:(NSDictionary *)where
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *whereStr = [self dictionaryToJSONString:where];
  whereStr = whereStr ? whereStr : @"{}";
  if (whereStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v count:type where:whereStr completionHandler:^(int count, NSHTTPURLResponse *response, NSError *error) {
          [self resolveRESTPromise:[NSNumber numberWithInt:count] response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(update:(NSString *)type id:(NSString *)ID object:(NSDictionary *)object
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *objectStr = [self dictionaryToJSONString:object];
  objectStr = objectStr ? objectStr : @"{}";
  if (objectStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v update:type id:ID object:objectStr completionHandler:^(NSDictionary *data, NSHTTPURLResponse *response, NSError *error) {
          data = data ? data : [[NSDictionary alloc] init];
          [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(publish:(NSString *)topic message:(NSDictionary *)message
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *messageStr = [self dictionaryToJSONString:message];
  messageStr = messageStr ? messageStr : @"{}";
  if (messageStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v publish:topic message:messageStr completionHandler:^(NSHTTPURLResponse *response, NSError *error) {
          [self resolveRESTPromise:nil response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(publishEvent:(NSString *)resource
  event:(NSString *)event message:(NSDictionary *)message
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *messageStr = [self dictionaryToJSONString:message];
  messageStr = messageStr ? messageStr : @"{}";
  if (messageStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
          [vui.v publishEvent:resource event:event message:messageStr completionHandler:^(NSHTTPURLResponse *response, NSError *error) {
          [self resolveRESTPromise:nil response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

// helper that performs the actual call to execute -- this is necessary since the Android
// version needs two versions of export: executeByName and executeByPositon
- (void)execute:(NSString *)procedure params:(id)params
    resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject {
    NSString *paramsStr = [self dictionaryToJSONString:params];
    paramsStr = paramsStr ? paramsStr : @"{}";
    if (paramsStr) {
      [vui ensureValidToken:^(NSDictionary *response) {
        BOOL authValid = [response objectForKey:@"authValid"];
        if (authValid) {
          [vui.v execute:procedure params:paramsStr completionHandler:^(NSDictionary *data, NSHTTPURLResponse *response, NSError *error) {
            data = data ? data : [[NSDictionary alloc] init];
            [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
          }];
        } else {
          [self sendAuthReject:reject];
        }
      }];
    } else {
      [self sendJSONReject:reject];
    }
}

RCT_EXPORT_METHOD(executeByName:(NSString *)procedure params:(id)params
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [self execute:procedure params:params resolver:resolve rejector:reject];
}
RCT_EXPORT_METHOD(executeByPosition:(NSString *)procedure params:(id)params
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [self execute:procedure params:params resolver:resolve rejector:reject];
}

RCT_EXPORT_METHOD(deleteOne:(NSString *)type id:(NSString *)ID
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [vui ensureValidToken:^(NSDictionary *response) {
    BOOL authValid = [response objectForKey:@"authValid"];
    if (authValid) {
      [vui.v deleteOne:type id:ID completionHandler:^(NSHTTPURLResponse *response, NSError *error) {
        [self resolveRESTPromise:nil response:response error:error resolver:resolve rejector:reject];
      }];
    } else {
      [self sendAuthReject:reject];
    }
  }];
}

RCT_EXPORT_METHOD(delete:(NSString *)type where:(NSDictionary *)where
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  NSString *whereStr = [self dictionaryToJSONString:where];
  whereStr = whereStr ? whereStr : @"{}";
  if (whereStr) {
    [vui ensureValidToken:^(NSDictionary *response) {
        BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v delete:type where:whereStr completionHandler:^(NSHTTPURLResponse *response, NSError *error) {
          [self resolveRESTPromise:nil response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
  } else {
    [self sendJSONReject:reject];
  }
}

RCT_EXPORT_METHOD(registerForPushNotifications:(RCTPromiseResolveBlock)resolve
  rejector:(RCTPromiseRejectBlock)reject) {
  [vui ensureValidToken:^(NSDictionary *response) {
      BOOL authValid = [response objectForKey:@"authValid"];
      if (authValid) {
        [vui.v registerForPushNotifications:APNSDeviceToken
          completionHandler:^(NSDictionary *data, NSHTTPURLResponse *response, NSError *error) {
          [self resolveRESTPromise:nil response:response error:error resolver:resolve rejector:reject];
        }];
      } else {
        [self sendAuthReject:reject];
      }
    }];
}

// see comment for execute above
- (void)executeStreamed:(NSString *)procedure params:(id)params
    maxBufferSize:(int)maxBufferSize maxFlushInterval:(int)maxFlushInterval
    progressEvent:(NSString *)progressEvent resolver:(RCTPromiseResolveBlock)resolve
    rejector:(RCTPromiseRejectBlock)reject {
    [vui ensureValidToken:^(NSDictionary *response) {
        BOOL authValid = [response objectForKey:@"authValid"];
        if (authValid) {
            NSString *paramsStr = [self dictionaryToJSONString:params];
            paramsStr = paramsStr ? paramsStr : @"{}";
            [vui.v executeStreamed:procedure params:paramsStr maxBufferSize:maxBufferSize maxFlushInterval:maxFlushInterval progressCallback:^(NSDictionary *progressDict) {
                [self sendEventWithName:progressEvent body:progressDict];
            } completionHandler:^(id data, NSHTTPURLResponse *response, NSError *error) {
                data = data ? data : [[NSDictionary alloc] init];
                [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
            }];
        } else {
            [self sendAuthReject:reject];
        }
    }];
}

RCT_EXPORT_METHOD(executeStreamedByName:(NSString *)procedure params:(id)params
  maxBufferSize:(int)maxBufferSize maxFlushInterval:(int)maxFlushInterval
  progressEvent:(NSString *)progressEvent resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
    [self executeStreamed:procedure params:params maxBufferSize:maxBufferSize maxFlushInterval:maxFlushInterval progressEvent:progressEvent resolver:resolve rejector:reject];
}
RCT_EXPORT_METHOD(executeStreamedByPosition:(NSString *)procedure params:(id)params
  maxBufferSize:(int)maxBufferSize maxFlushInterval:(int)maxFlushInterval
  progressEvent:(NSString *)progressEvent resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [self executeStreamed:procedure params:params maxBufferSize:maxBufferSize maxFlushInterval:maxFlushInterval progressEvent:progressEvent resolver:resolve rejector:reject];
}

// see comment for execute above
- (void)executePublic:(NSString *)namespace procedure:(NSString *)procedure params:(id)params
    resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject {
    [vui ensureValidToken:^(NSDictionary *response) {
        BOOL authValid = [response objectForKey:@"authValid"];
        if (authValid) {
            NSString *paramsStr = [self dictionaryToJSONString:params];
            paramsStr = paramsStr ? paramsStr : @"{}";
            [vui.v publicExecute:namespace procedure:procedure params:paramsStr completionHandler:^(id data, NSHTTPURLResponse *response, NSError *error) {
                data = data ? data : [[NSDictionary alloc] init];
                [self resolveRESTPromise:data response:response error:error resolver:resolve rejector:reject];
            }];
        } else {
            [self sendAuthReject:reject];
        }
    }];
}

RCT_EXPORT_METHOD(executePublicByName:(NSString *)namespace procedure:(NSString *)procedure params:(id)params
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [self executePublic:namespace procedure:procedure params:params resolver:resolve rejector:reject];
}
RCT_EXPORT_METHOD(executePublicByPosition:(NSString *)namespace procedure:(NSString *)procedure params:(id)params
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
  [self executePublic:namespace procedure:procedure params:params resolver:resolve rejector:reject];
}

/*** Exported methods for creating new users
 **/
RCT_EXPORT_METHOD(createOAuthUser:(NSString *)urlScheme clientId:(NSString *)clientId
  resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject) {
    [vui ensureValidToken:^(NSDictionary *response) {
        BOOL authValid = [response objectForKey:@"authValid"];
        if (authValid) {
            [vui createOAuthUser:urlScheme clientId:clientId completionHandler:^(NSDictionary * _Nonnull response) {
                NSString *errorStr = [response objectForKey:@"errorStr"];
                if (!errorStr.length) {
                    resolve(response);
                } else {
                    reject(veRESTError, errorStr, [self buildNSError:response]);
                }
            }];
        } else {
            [self sendAuthReject:reject];
        }
    }];
}

RCT_EXPORT_METHOD(createInternalUser:(NSString *)username password:(NSString *)password
 email:(NSString *)email firstName:(NSString *)firstName lastName:(NSString *)lastName
  phone:(NSString *)phone resolver:(RCTPromiseResolveBlock)resolve
  rejector:(RCTPromiseRejectBlock)reject) {
    [vui ensureValidToken:^(NSDictionary *response) {
        BOOL authValid = [response objectForKey:@"authValid"];
        if (authValid) {
            [vui createInternalUser:username password:password email:email firstName:firstName lastName:lastName phone:phone completionHandler:^(NSDictionary * _Nonnull response) {
                NSString *errorStr = [response objectForKey:@"errorStr"];
                if (!errorStr.length) {
                    resolve(response);
                } else {
                    reject(veRESTError, errorStr, [self buildNSError:response]);
                }
            }];
        } else {
            [self sendAuthReject:reject];
        }
    }];
}

/*** Exported methods for using React Event Emitter events
 **/
// mandatory methods for sub-classed RCTEventEmitter class
RCT_EXPORT_METHOD(addListener:(NSString *)eventName) {
    [super addListener:eventName];
}
//RCT_EXPORT_METHOD(removeListeners:(nonnull NSNumber *)count) {
RCT_EXPORT_METHOD(removeListeners:(double)count) {
    [super removeListeners:count];
}

// need to use registerSupportedEvents before allocating the Event Emitter
RCT_EXPORT_METHOD(registerSupportedEvents:(NSArray<NSString *> *)_supportedEvents) {
    supportedEvents = _supportedEvents;
}
- (NSArray<NSString *> *)supportedEvents {
    return supportedEvents;
}
@end
