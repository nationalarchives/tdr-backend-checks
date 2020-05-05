import boto3
import pytest
from moto import mock_s3
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
        yield boto3.resource('s3', region_name='us-east-1')


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
                    "object": {"key": key}
                }
            }
        )
    return {
        "Records": records
    }


@mock_s3
def test_load_is_called(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    yara.load.assert_called_once_with("output")


@mock_s3
def test_correct_output(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)

    assert res[0]["software"] == "yara"
    assert res[0]["softwareVersion"] == yara.__version__
    assert res[0]["databaseVersion"] == version.version


@mock_s3
def test_match_found(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')

    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)

    assert res[0]["result"] == "testmatch"


@mock_s3
def test_no_match_found(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesNoMatch()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    assert res[0]["result"] == ""


@mock_s3
def test_multiple_match_found(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMultipleMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)
    assert res[0]["result"] == "testmatch\ntestmatch"


@mock_s3
def test_multiple_records(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler(get_records("testbucket", "test", 2), None)
    assert len(res) == 2
    assert res[0]["result"] == "testmatch"
    assert res[1]["result"] == "testmatch"


@mock_s3
def test_bucket_not_found(s3, mocker):
    with pytest.raises(s3.meta.client.exceptions.NoSuchBucket):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesNoMatch()
        matcher.matcher_lambda_handler(get_records("anotherbucket", "another_test"), None)


@mock_s3
def test_key_not_found(s3, mocker):
    with pytest.raises(s3.meta.client.exceptions.NoSuchKey):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesNoMatch()
        matcher.matcher_lambda_handler(get_records("testbucket", "another_test"), None)


@mock_s3
def test_match_fails(s3, mocker):
    with pytest.raises(yara.Error):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesMatchError()
        matcher.matcher_lambda_handler(get_records("testbucket", "test"), None)


def test_no_records():
    res = matcher.matcher_lambda_handler({}, None)
    assert res == []
