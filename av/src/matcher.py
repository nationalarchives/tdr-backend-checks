import yara
import boto3
import json
import logging
from .version import version
from datetime import datetime
import os

FORMAT = '%(asctime)-15s %(message)s'
INFO = 20
logging.basicConfig(format=FORMAT)
logger = logging.getLogger('matcher')
logger.setLevel(INFO)


def matcher_lambda_handler(event, lambda_context):
    if "Records" in event:
        outputs = []
        sqs_client = boto3.client("sqs")
        s3_client = boto3.client("s3")
        for record in event["Records"]:
            s3 = record["s3"]
            bucket = s3["bucket"]["name"]
            key = s3["object"]["key"]
            logger.info("Object %s found", key)

            s3_object = s3_client.get_object(Bucket=bucket, Key=key)
            streaming_body = s3_object["Body"]

            rules = yara.load("output")
            match = rules.match(data=streaming_body.read())
            result = "\n".join([x.rule for x in match])
            time = datetime.today().strftime("%Y-%m-%d-%H:%M:%S")
            output = {"software": "yara", "softwareVersion": yara.__version__, "databaseVersion": version,
                      "result": result,
                      "datetime": time}
            outputs.append(output)
            logger.info("Key %s processed", key)

            sqs_client.send_message(QueueUrl=os.environ["SQS_URL"], MessageBody=json.dumps(output))
        return outputs
    else:
        logger.info("Message does not contain any records")
        return []
