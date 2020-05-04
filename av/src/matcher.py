import yara
import boto3
import json


def matcher_lambda_handler(event, lambda_context):
    bucket = event["bucketName"]
    key = event["key"]
    rules = yara.load("output")
    s3 = boto3.client("s3")
    s3_object = s3.get_object(Bucket=bucket, Key=key)
    streaming_body = s3_object["Body"]
    match = rules.match(data=streaming_body.read())
    result = [x.rule for x in match]
    return json.dumps({"result": result})

