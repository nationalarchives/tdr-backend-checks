## API Update lambdas
There will be a separate project for each different file metadata update message as well as a common project for shared code. So for now, this is one for the antivirus, the file format and the checksum. 

### Running the tests 
This is all from the `api-update` directory

To run the common tests

`sbt test`

To run the other project tests, e.g. for the antivirus

`sbt antivirus/test`

To build the jar

`sbt antivirus/assembly`

To run the tests in IntelliJ, you will need to set environment variables. These are set in the sbt file which isn't read by intellij
```
"API_URL" -> "http://localhost:9001/graphql" 
"AUTH_URL" -> "http://localhost:9002/auth" 
"CLIENT_ID" -> "id" 
"CLIENT_SECRET" -> "secret"
```

