import React, {useState} from 'react'
import { NativeModules, StyleSheet, Pressable, Text, View, TextInput, ScrollView } from "react-native";
import { RootSiblingParent } from 'react-native-root-siblings';

const {VantiqReact} = NativeModules;
var transcriptText = "";
var doInit = true;
var VANTIQ_SERVER = 'https://staging.vantiq.com';
var VANTIQ_NAMESPACE = 'swan'

export default function Index() {
    const [transcript, setTranscript] = useState(transcriptText);
    const [authVisible, setAuthVisible] = useState(false);
    const [startVisible, setStartVisible] = useState(true);
    const [internalUsername, setInternalUsername] = useState('');
    const [internalPassword, setInternalPassword] = useState('');

    // user presses the Start Running button
    function onButtonPress() {
        setStartVisible(false);
        if (doInit) {
            // only do the init once so we can run the tests multiple times
            doInit = false;
            VantiqReact.init(VANTIQ_SERVER, VANTIQ_NAMESPACE).then(
             function(response) {
                 if (response.authValid == "true") {
                     runTests();
                 } else {
                     // authentication error so need to authenticate
                     if (response.serverType == "Internal") {
                         setAuthVisible(true);
                     } else if (response.serverType == "OAuth") {
                         VantiqReact.authWithOAuth('vantiqreact', 'vantiqReact').then(
                            function(response) {
                               if (response.authValid == "true") {
                                   runTests();
                               } else {
                                   addToTranscript('Authentication error: ' + response.errorStr);
                                   setStartVisible(true);
                               }
                            }, function(error) {
                               addToTranscript('Authentication error: ' + response.errorStr);
                               setStartVisible(true);
                            });
                     } else {
                         addToTranscript('Invalid server type');
                         setStartVisible(true);
                     }
                 }
             }, function(error) {
                 addToTranscript('init fail: ' + response.errorStr);
                 setStartVisible(true);
             });
        } else {
            runTests();
        }
    }
    
    // used for logging in to an Internal auth server, after the user has entered
    // a username and password (internalUsername, internalPassword)
    function onLoginPress() {
        setAuthVisible(false);
        VantiqReact.authWithInternal(internalUsername, internalPassword).then(
            function(response) {
               if (response.authValid == "true") {
                   runTests();
               } else {
                   addToTranscript('Authentication error: ' + response.errorStr);
                   setAuthVisible(true);
               }
            }, function(error) {
                setAuthVisible(true);
                addToTranscript('Authentication error: ' + error.message + ", code: " + error.code);
            });
    }
    
    // helper to add any error string and code to the transcript
    reportTestError = function(op, error) {
        addToTranscript(op + ' error: ' + error.message + ", code: " + error.code);
    }
    
    var lastVantiqID;
    runTests = function() {
        VantiqReact.select('system.types', [], {}, {}, -1).then(
            function(data) {
                var length = data ? data.length : "N/A";
                addToTranscript('Select returns ' + length + ' items.');
            }, function(error) {
                reportTestError('Select', error);
            });
        setTimeout(function() {
            VantiqReact.count('system.types', {"ars_version":{"$gt":5}}).then(
                 function(count) {
                     addToTranscript('Count returns ' + count + ' items.');
                 }, function(error) {
                     reportTestError('Count', error);
                 });
        }, 2000);
        setTimeout(function() {
            VantiqReact.insert('TestType', {"intValue":42,"uniqueString":"42"}).then(
                function(data) {
                    data = data ? data : {};
                    lastVantiqID = (data._id).toString();
                    addToTranscript('Insert successful, id: ' + lastVantiqID + '.');
                }, function(error) {
                    reportTestError('Insert', error);
                });
        }, 4000);
        setTimeout(function() {
            VantiqReact.upsert('TestType', {"intValue":44,"uniqueString":"A Unique String."}).then(
                function(data) {
                    addToTranscript('Upsert successful.');
                }, function(error) {
                    reportTestError('Upsert', error);
                });
        }, 6000);
        setTimeout(function() {
            VantiqReact.upsert('TestType', {"intValue":45,"uniqueString":"A Unique String."}).then(
                function(data) {
                    addToTranscript('Upsert successful.');
                }, function(error) {
                    reportTestError('Upsert', error);
                });
        }, 8000);
        setTimeout(function() {
            VantiqReact.update('TestType', lastVantiqID, {"stringValue":"Updated String."}).then(
                function(data) {
                    addToTranscript('Update successful.');
                }, function(error) {
                    reportTestError('Update', error);
                });
        }, 10000);
        setTimeout(function() {
            VantiqReact.selectOne('TestType', lastVantiqID).then(
                function(data) {
                    data = data ? data : [];
                    addToTranscript('SelectOne successful: ' + data.length + ' records.');
                }, function(error) {
                    reportTestError('SelectOne', error);
                });
        }, 12000);
        setTimeout(function() {
            VantiqReact.publish('/vantiq', {"intValue":42}).then(
                function(data) {
                    addToTranscript('Publish successful.');
                }, function(error) {
                    reportTestError('Publish', error);
                });
        }, 14000);
        setTimeout(function() {
            VantiqReact.execute('sumTwo', [35, 21]).then(
                function(data) {
                    addToTranscript('Execute successful: ' + JSON.stringify(data));
                }, function(error) {
                    reportTestError('Execute', error);
                });
        }, 16000);
        setTimeout(function() {
            VantiqReact.deleteOne('TestType', lastVantiqID).then(
                function(data) {
                    addToTranscript('DeleteOne successful.');
                }, function(error) {
                    reportTestError('DeleteOne', error);
                });
        }, 18000);
        setTimeout(function() {
            VantiqReact.delete('TestType', {"intValue":42}).then(
                function(data) {
                    addToTranscript('Delete successful.');
                }, function(error) {
                    reportTestError('Delete', error);
                });
        }, 20000);
        setTimeout(function() {
            VantiqReact.select('system.types', ["name", "_id"], {}, {"name":-1}, -1).then(
                function(data) {
                    var length = data ? data.length : "N/A";
                    addToTranscript('Select returns ' + length + ' items.');
                }, function(error) {
                    reportTestError('Select', error);
                });
        }, 22000);
        setTimeout(function() {
            VantiqReact.delete('TestType', {"intValue":42}).then(
                function(data) {
                    addToTranscript('Delete successful.');
                    addToTranscript('Tests complete.');
                    setStartVisible(true);
                }, function(error) {
                    reportTestError('Delete', error);
                    addToTranscript('Tests complete.');
                    setStartVisible(true);
                });
        }, 24000);
    }
    
    addToTranscript = function(text) {
        transcriptText += '\n' + text;
        setTranscript(transcriptText);
    }

    return (
        <RootSiblingParent>
            <View style={styles.container}>
                <Text style={styles.text}>Vantiq React Sample</Text>
                {startVisible ?
                    (<Pressable style={styles.button} onPress={onButtonPress}>
                        <Text style={styles.text}>Start Running</Text>
                    </Pressable>) : null
                }
                {authVisible ?
                    (<TextInput style={styles.textInput} placeholder="Username" value={internalUsername} onChangeText={setInternalUsername} autoCapitalize='none' autoCorrect={false}></TextInput>) : null}
                {authVisible ?
                    (<TextInput style={styles.textInput} placeholder="Password" value={internalPassword} onChangeText={setInternalPassword} autoCapitalize='none' autoCorrect={false} secureTextEntry={true}></TextInput>) : null}
                {authVisible ?
                     (<Pressable style={styles.button} onPress={onLoginPress}>
                        <Text style={styles.text}>Login</Text>
                     </Pressable>) : null
                }
                <ScrollView ref={ref => {this.scrollView = ref}}
                    onContentSizeChange={() => this.scrollView.scrollToEnd({animated: true})}>
                    <Text style={styles.transcript}>{transcript}</Text>
                </ScrollView>
            </View>
        </RootSiblingParent>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    marginVertical: 80,
  },
  text: {
    color: 'lightgrey',
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
    backgroundColor: 'transparent',
  },
  button: {
    backgroundColor: '#3B86F7',
    color: 'white',
    marginVertical: 12,
    marginHorizontal: 100
  },
  transcript: {
    backgroundColor: 'white',
    color: 'black',
    fontSize: 14,
    marginHorizontal: 10
  },
  textInput: {
    borderColor: 'lightgrey',
    borderWidth: 1,
    marginTop: 10,
    borderRadius: 4,
    color: 'black',
    marginHorizontal: 80,
    height: 30,
    fontSize: 16,
  },
});
