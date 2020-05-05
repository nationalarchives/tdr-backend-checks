import boto3
import pytest
from moto import mock_s3, mock_sqs
import os
from src import matcher
from src import version
import yara


@pytest.fixture(scope='function')
def aws_credentials():
    """Mocked AWS Credentials for moto."""
    os.environ['AWS_ACCESS_KEY_ID'] = 'testing'
    os.environ['AWS_SECRET_ACCESS_KEY'] = 'testing'
    os.environ['AWS_SECURITY_TOKEN'] = 'testing'
    os.environ['AWS_SESSION_TOKEN'] = 'testing'


@pytest.fixture(scope='function')
def s3(aws_credentials):
    with mock_s3():
        yield boto3.resource('s3', region_name='eu-west-2')


@pytest.fixture(scope='function')
def sqs(aws_credentials):
    with mock_sqs():
        yield boto3.client('sqs', region_name='eu-west-2')


class MockMatch:
    rule = "testmatch"


class MockRulesMatchFound:

    @staticmethod
    def match(data):
        return [MockMatch()]


class MockRulesMultipleMatchFound:

    @staticmethod
    def match(data):
        return [MockMatch(), MockMatch()]


class MockRulesNoMatch:

    @staticmethod
    def match(data):
        return []


class MockRulesMatchError:

    @staticmethod
    def match(data):
        raise yara.Error()


def get_records(bucket, key, num=1):
    records = []
    for i in range(num):
        records.append(
            {
                "s3": {
                    "bucket": {"name": bucket},
                    "object": {"key": key + str(i)}
                }
            }
        )
    return {
        "Records": records
    }


def test_load_is_called(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    yara.load.assert_called_once_with("output")


def test_correct_output(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)

    assert res[0]["software"] == "yara"
    assert res[0]["softwareVersion"] == yara.__version__
    assert res[0]["databaseVersion"] == version.version


def test_match_found(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    mocker.patch('yara.load')

    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)

    assert res[0]["result"] == "testmatch"


def test_no_match_found(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesNoMatch()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    assert res[0]["result"] == ""


def test_multiple_match_found(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMultipleMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    assert res[0]["result"] == "testmatch\ntestmatch"


def test_multiple_records(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    s3.Object("testbucket", "test1").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test", 2), None)
    assert len(res) == 2
    assert res[0]["result"] == "testmatch"
    assert res[1]["result"] == "testmatch"


def test_bucket_not_found(s3, sqs, mocker):
    with pytest.raises(s3.meta.client.exceptions.NoSuchBucket):
        os.environ["ENVIRONMENT"] = "intg"
        os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
        sqs.create_queue(QueueName="tdr-api-update-intg")
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test0").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesNoMatch()
        matcher.matcher_lambda_handler(get_records("anotherbucket", "another_test"), None)


def test_key_not_found(s3, sqs, mocker):
    with pytest.raises(s3.meta.client.exceptions.NoSuchKey):
        os.environ["ENVIRONMENT"] = "intg"
        os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
        sqs.create_queue(QueueName="tdr-api-update-intg")
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test0").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesNoMatch()
        matcher.matcher_lambda_handler(get_records("testbucket", "another_test"), None)


def test_match_fails(s3, sqs, mocker):
    with pytest.raises(yara.Error):
        os.environ["ENVIRONMENT"] = "intg"
        os.environ["SQS_URL"] = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
        sqs.create_queue(QueueName="tdr-api-update-intg")
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test0").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesMatchError()
        matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)


def test_no_records():
    res = matcher.matcher_lambda_handler({}, None)
    assert res == []


def test_output_sent_to_queue(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    queue_url = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    os.environ["SQS_URL"] = queue_url
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    res = sqs.receive_message(QueueUrl=queue_url)
    print(res["Messages"])
    assert len(res["Messages"]) == 1


def test_output_sent_to_queue_multiple_records(s3, sqs, mocker):
    os.environ["ENVIRONMENT"] = "intg"
    queue_url = "https://queue.amazonaws.com/123456789012/tdr-api-update-intg"
    os.environ["SQS_URL"] = queue_url
    sqs.create_queue(QueueName="tdr-api-update-intg")
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test0").put(Body="test")
    s3.Object("testbucket", "test1").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    matcher.matcher_lambda_handler(get_records("testbucket", "test", 2), None)
    res = sqs.receive_message(QueueUrl=queue_url, MaxNumberOfMessages=11)
    messages = res["Messages"]
    # while "Messages" in res and len(res["Messages"]) > 0:
    assert len(messages) == 2
