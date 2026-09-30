# Staging deployment

Use Java 17+ on the computer for the build. All AWS API calls run with boto3 through
the managed AWS MCP Server (`aws___run_script`) under `DextoDemoRole`; there are no
local AWS credentials and no AWS CLI. Region is `us-east-1`. The `ops-console`
application and single-instance `ops-console-staging` environment are provisioned
by demo-infra on **Corretto 17 running on 64bit Amazon Linux 2023**.

1. Commit changes, record the full SHA, and build:

```sh
./mvnw -B package
```

2. Resolve the account with boto3 `sts.get_caller_identity()["Account"]` in
   `run_script`. Use `aws___get_presigned_url` to obtain an S3 **PUT** URL for bucket
   `brahma-demo-artifacts-<account>`, key `ops-console/<sha>.jar`. Upload the actual
   executable JAR locally using that URL (do not expose the signed URL in logs):

```sh
curl --fail --upload-file target/ops-console.jar "$PRESIGNED_PUT_URL"
```

3. Call these AWS APIs via `run_script` (no deployment CLI):

```python
import boto3
import time

session = boto3.Session(region_name="us-east-1")
account = session.client("sts").get_caller_identity()["Account"]
sha = "<sha>"
eb = session.client("elasticbeanstalk")
eb.create_application_version(
    ApplicationName="ops-console", VersionLabel=sha,
    SourceBundle={"S3Bucket": f"brahma-demo-artifacts-{account}",
                  "S3Key": f"ops-console/{sha}.jar"},
    Process=True,
)
eb.update_environment(
    EnvironmentName="ops-console-staging", VersionLabel=sha,
    OptionSettings=[
        {"Namespace": "aws:elasticbeanstalk:application:environment",
         "OptionName": "SERVER_PORT", "Value": "5000"},
        {"Namespace": "aws:elasticbeanstalk:application:environment",
         "OptionName": "ACCOUNTS_API_URL", "Value": "http://<accounts-api-load-balancer-hostname>"},
        {"Namespace": "aws:elasticbeanstalk:application",
         "OptionName": "Application Healthcheck URL", "Value": "/healthz"},
    ],
)
for _ in range(120):
    env = eb.describe_environments(EnvironmentNames=["ops-console-staging"])["Environments"][0]
    if env["Status"] == "Ready" and env["Health"] == "Green" and env["VersionLabel"] == sha:
        print({"url": f"http://{env['CNAME']}"})
        break
    if env["Status"] in {"Terminated", "Terminating"}:
        raise RuntimeError(env["Status"])
    time.sleep(10)
else:
    raise TimeoutError("Environment not Ready/Green on expected revision; inspect EB events")
```

Return to separate `run_script` calls to poll if the server's execution timeout is
shorter than deployment. `ACCOUNTS_API_URL` **must** be set as an EB environment
property to the deployed accounts-api URL. `/healthz` alone does not test that
connection. Set the environment properties and health path explicitly as above.

4. Read the environment's CNAME from `describe_environments`, then run the live
   smoke locally against it:

```sh
cd e2e
npm ci
npx playwright install chromium
BASE_URL=http://<environment-cname> npm test
```

The smoke checks the directory, industry filter, name search, and account detail.
