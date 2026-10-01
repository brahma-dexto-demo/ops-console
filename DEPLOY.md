# Staging deployment

Build and publish the JAR inside AWS CodeBuild with Corretto 17. All deployer AWS
API calls run with boto3 through the managed AWS MCP Server (`aws___run_script`) under `DextoDemoRole`; there are no
local AWS credentials and no AWS CLI. Region is `us-east-1`. The `ops-console`
application and single-instance `ops-console-staging` environment are provisioned
by demo-infra on **Corretto 17 running on 64bit Amazon Linux 2023**.

1. Commit changes, record the full SHA, and publish that revision through GitHub
   so CodeBuild can fetch it. Use `aws___run_script` to start `ops-console-jar` at
   that SHA. The project's `ARTIFACTS_BUCKET` comes from demo-infra; its service
   role publishes the JAR with AWS CLI inside the build container. Buildspec
   selects Corretto 17 and only builds/publishes; it does not deploy.

```python
import boto3

sha = "<sha>"  # Full published source revision
codebuild = boto3.Session(region_name="us-east-1").client("codebuild")
build_id = codebuild.start_build(
    projectName="ops-console-jar", sourceVersion=sha,
)["build"]["id"]
print({"build_id": build_id})
```

2. Poll in subsequent `aws___run_script` calls using the returned build ID:

```python
import boto3

sha = "<sha>"
build_id = "<build-id>"
codebuild = boto3.Session(region_name="us-east-1").client("codebuild")
build = codebuild.batch_get_builds(ids=[build_id])["builds"][0]
status = build["buildStatus"]
if status in {"FAILED", "FAULT", "STOPPED", "TIMED_OUT"}:
    raise RuntimeError(f"Build {build_id}: {status}; inspect build['logs']")
if status == "SUCCEEDED":
    if build["resolvedSourceVersion"] != sha:
        raise RuntimeError("Build source does not match the requested SHA")
    print({"build_id": build_id, "status": status,
           "s3_key": f"ops-console/{build['resolvedSourceVersion']}.jar"})
else:
    print({"build_id": build_id, "status": status})  # Resume polling
```

   Require `SUCCEEDED` and the expected SHA before proceeding. The printed S3 key
   is the source bundle for the EB application version below; retain it and the
   build ID in the delivery report.

3. Set the API URL through CloudFormation `UpdateStack` on `brahma-demo-staging`
   via `aws___run_script`. `AccountsApiUrl` is the source of truth for
   `ACCOUNTS_API_URL`; the stack also owns port 5000 and health path `/healthz`.
   Supply the bootstrap execution-role output and the observed API Service URL:

```python
import boto3

session = boto3.Session(region_name="us-east-1")
cfn = session.client("cloudformation")
stack = cfn.describe_stacks(StackName="brahma-demo-staging")["Stacks"][0]
api_url = "http://<accounts-api-load-balancer-hostname>"
parameters = [
    {"ParameterKey": p["ParameterKey"], "ParameterValue": api_url}
    if p["ParameterKey"] == "AccountsApiUrl"
    else {"ParameterKey": p["ParameterKey"], "UsePreviousValue": True}
    for p in stack["Parameters"]
]
if next(p["ParameterValue"] for p in stack["Parameters"]
        if p["ParameterKey"] == "AccountsApiUrl") != api_url:
    cfn.update_stack(
        StackName="brahma-demo-staging", UsePreviousTemplate=True,
        RoleARN="<DextoDemoCfnExecRoleArn>",
        Capabilities=["CAPABILITY_NAMED_IAM"], Parameters=parameters,
        Tags=stack.get("Tags", []),
    )
```

   Poll `describe_stacks` in subsequent `run_script` calls until `UPDATE_COMPLETE`;
   report stack events on failure. If the URL already matches, skip the update
   and require a stable `CREATE_COMPLETE` or `UPDATE_COMPLETE` stack. Retain every
   other parameter with `UsePreviousValue=true` and retain stack tags. Then deploy
   the application version through `run_script`:

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
shorter than deployment. CloudFormation supplies `ACCOUNTS_API_URL` from
`AccountsApiUrl`; application deployments change only `VersionLabel` through EB.
`/healthz` alone does not test the API connection.

4. Read the environment's CNAME from `describe_environments`, then run the live
   smoke locally against it:

```sh
cd e2e
npm ci
npx playwright install chromium
BASE_URL=http://<environment-cname> npm test
```

The smoke checks the directory, industry filter, name search, and account detail.
