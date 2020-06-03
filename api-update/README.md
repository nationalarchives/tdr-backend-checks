## API Update lambdas
There is a separate project for each different file metadata update message as well as a common project for shared code. 

### Running the tests 
This is all from the `api-update` directory

To run the common tests

`sbt test`

To run the other project tests, e.g. for the antivirus

`sbt antivirus/test`

To build the jar

`sbt antivirus/assembly`
