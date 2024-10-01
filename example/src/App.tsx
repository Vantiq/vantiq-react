
import { StyleSheet, View, Button } from 'react-native';
import { initialize, multiply, add, testOne } from 'vantiq-interface-library';


export default function App() {


  const onAdd = () => {
    let a:number = 5;
    let b:number = 7;

    console.log('Invoke Add');

    add(a,b).then(function(value:number)
     {
       console.log(`Add ${a} + ${b} = ${value}`);
     },
     function(err:string)
     {
       console.log(`Error ${err}`);
     });
  };

  const onMultiply = () => {
    let a:number = 2;
    let b:number = 6;

    console.log('Invoke Multiply');

    multiply(a,b).then(function(value:number)
        {
          console.log(`Multiply ${a} * ${b} = ${value}`);
        },
        function(err:string)
        {
          console.log(`Error ${err}`);
        });
  };

  const onInitialize= () => {

    console.log('Invoke Initialize');

    let server:string = "http://10.0.0.208:8080";
    let namespace:string = "Scratch1";

    //server = "https://test.vantiq.com";
    //namespace = "SteveNS1";

    
    initialize(server,namespace).then(function(value:any)
        {
          console.log("Initialize");
        },
        function(err:string)
        {
          console.log("Initialize FAIL");
        });

  };
  
  const onTest1= () => {

    console.log('Invoke Test1');


    testOne().then(function(results:string)
                      {
                        console.log(`TestOne Results = ${results}`);
                      },
                      function(err:string)
                      {
                        console.log(`Error ${err}`);
                      });
  };

  return (
    <View style={styles.container}>

      <Button
        title="Click to Initialize"
        color="#555555"
        onPress={onInitialize}
      />
      
      <Button
        title="Click to Multiply"
        color="#00ff00"
        onPress={onMultiply}
      />
      <Button
        title="Click to Add"
        color="#ff0000"
        onPress={onAdd}
      />

      <Button
        title="Test 1"
        color="#00ffff"
        onPress={onTest1}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    rowGap:20
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
