This is the code and configuration to carry out the antivirus checks on a single file from S3

There will be a task to automatically build the virus definitions into the lambda but for now it's a manual process.
First, set $MANAGEMENT_ACCOUNT, STAGE and $YARA_VERSION. Current version as I write this is 4.0.0
```
export MANAGEMENT_ACCOUNT=managementaccount
export YARA_VERSION=4.0.0
export STAGE=intg
```

Build the base yara image
`docker build -f Dockerfile-yara --build-arg YARA_VERSION=$YARA_VERSION -t $MANAGEMENT_ACCOUNT.dkr.ecr.eu-west-2.amazonaws.com/yara:$STAGE .`

Build the dependencies image

`docker build -f Dockerfile-dependencies --build-arg YARA_VERSION=$YARA_VERSION -t $MANAGEMENT_ACCOUNT.dkr.ecr.eu-west-2.amazonaws.com/yara-dependencies:$STAGE .`

Build the rules file container

`docker build -f Dockerfile-compile -t yara-rules --build-arg STAGE=$STAGE --build-arg ACCOUNT_NUMBER=$MANAGEMENT_ACCOUNT .`

Run the dependencies container

`docker run -itd --rm --name dependencies $MANAGEMENT_ACCOUNT.dkr.ecr.eu-west-2.amazonaws.com/yara-dependencies:$STAGE`

Copy the dependencies zip locally

`docker cp dependencies:/lambda/dependencies.zip .`

Run the rules container

`docker run -itd --rm --name rules yara-rules`

Make a lambda directory

`mkdir lambda`

Copy the rules file into the lambda directory

`docker cp rules:/rules/output ./lambda`

Unzip the dependencies into the lambda directory

`unzip -q dependencies.zip -d ./lambda`

Copy main.py to the lambda directory

`cp main.py ./lambda`

Enter the lambda directory 

`cd lambda`

Zip the contents

`zip -r9 function.zip .`

Upload to S3

`aws s3 cp function.zip s3://tdr-backend-checks-staging/yara-av.zip`

Update the lambda function. You will need credentials for whichever environment you're deploying to

`aws lambda update-function-code --function-name yara-av-intg --s3-bucket tdr-backend-checks --s3-key yara-av.zip` 

Clean up
```
cd ..
rm -rf lambda dependencies.zip
docker stop rules dependencies
```

