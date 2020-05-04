import boto3
import pytest
from moto import mock_s3
import os
from src import matcher
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
    rule = "test"


class MockRulesMatchFound:

    @staticmethod
    def match(data):
        return [MockMatch()]


class MockRulesNoMatch:

    @staticmethod
    def match(data):
        return []


class MockRulesMatchError:

    @staticmethod
    def match(data):
        raise yara.Error()


@mock_s3
def test_match_found(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesMatchFound()
    res = matcher.matcher_lambda_handler({"bucketName": "testbucket", "key": "test"}, None)
    assert res == '{"result": ["test"]}'


@mock_s3
def test_no_match_found(s3, mocker):
    s3.create_bucket(Bucket='testbucket')
    s3.Object("testbucket", "test").put(Body="test")
    mocker.patch('yara.load')
    yara.load.return_value = MockRulesNoMatch()
    res = matcher.matcher_lambda_handler({"bucketName": "testbucket", "key": "test"}, None)
    assert res == '{"result": []}'


@mock_s3
def test_bucket_not_found(s3, mocker):
    with pytest.raises(s3.meta.client.exceptions.NoSuchBucket):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesNoMatch()
        matcher.matcher_lambda_handler({"bucketName": "anotherbucket", "key": "another_test"}, None)


@mock_s3
def test_key_not_found(s3, mocker):
    with pytest.raises(s3.meta.client.exceptions.NoSuchKey):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesNoMatch()
        matcher.matcher_lambda_handler({"bucketName": "testbucket", "key": "another_test"}, None)


@mock_s3
def test_rule_file_not_found(s3, mocker):
    with pytest.raises(yara.Error):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        matcher.matcher_lambda_handler({"bucketName": "testbucket", "key": "test"}, None)


@mock_s3
def test_match_fails(s3, mocker):
    with pytest.raises(yara.Error):
        s3.create_bucket(Bucket='testbucket')
        s3.Object("testbucket", "test").put(Body="test")
        mocker.patch('yara.load')
        yara.load.return_value = MockRulesMatchError()
        matcher.matcher_lambda_handler({"bucketName": "testbucket", "key": "test"}, None)
