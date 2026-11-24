"""Provision de cinco EC2 del laboratorio autorizado. Requiere boto3.

Lee credenciales del entorno AWS. Estado privado en .deploy-state, fuera de Git.
No elimina recursos. Ejecutar --account con el ID del laboratorio nuevo.
"""
import argparse
import base64
import json
import secrets
from pathlib import Path

import boto3
from botocore.exceptions import ClientError

ROOT = Path(__file__).resolve().parents[1]
STATE_DIR = ROOT / ".deploy-state"
PROJECT = "pedidos360-ep6"
NODES = ("frontend", "core", "support", "infra", "streaming")
CORE = ["ms-productos", "ms-clientes", "ms-pedidos", "notifications-service", "ms-auditoria"]
SUPPORT = ["bff-pedidos360", "rabbit-admin-service", "ms-pagos", "ms-analytics", "kafka-admin-service"]


def save(state):
    STATE_DIR.mkdir(exist_ok=True)
    target = STATE_DIR / "state.json"
    temporary = target.with_suffix(".tmp")
    temporary.write_text(json.dumps(state, indent=2), encoding="utf-8")
    temporary.replace(target)


def tags(name):
    return [{"Key": "Project", "Value": PROJECT}, {"Key": "Name", "Value": name}]


def provision(session, state):
    ec2 = session.client("ec2")
    vpc = ec2.describe_vpcs(Filters=[{"Name": "is-default", "Values": ["true"]}])["Vpcs"][0]["VpcId"]
    subnets = sorted(ec2.describe_subnets(Filters=[{"Name": "vpc-id", "Values": [vpc]}])["Subnets"],
                     key=lambda subnet: subnet["AvailabilityZone"])
    state.update(vpc=vpc, subnets=[subnets[0]["SubnetId"], subnets[1]["SubnetId"]])
    groups = state.setdefault("groups", {})
    for role in (*NODES, "alb", "vpclink"):
        name = f"p360-ep6-{role}"
        existing = ec2.describe_security_groups(Filters=[
            {"Name": "vpc-id", "Values": [vpc]}, {"Name": "group-name", "Values": [name]}])["SecurityGroups"]
        if existing:
            if not any(t["Key"] == "Project" and t["Value"] == PROJECT for t in existing[0].get("Tags", [])):
                raise RuntimeError(f"Grupo ajeno con nombre {name}; no modificar.")
            groups[role] = existing[0]["GroupId"]
        else:
            groups[role] = ec2.create_security_group(
                GroupName=name, Description=f"Pedidos360 EP6 {role}", VpcId=vpc,
                TagSpecifications=[{"ResourceType": "security-group", "Tags": tags(name)}])["GroupId"]
        save(state)

    def allow(role, port, source=None):
        permission = {"IpProtocol": "tcp", "FromPort": port, "ToPort": port}
        if source:
            permission["UserIdGroupPairs"] = [{"GroupId": groups[source]}]
        else:
            permission["IpRanges"] = [{"CidrIp": "0.0.0.0/0"}]
        try:
            ec2.authorize_security_group_ingress(GroupId=groups[role], IpPermissions=[permission])
        except ClientError as error:
            if error.response["Error"]["Code"] != "InvalidPermission.Duplicate":
                raise

    for port in (80, 443):
        allow("frontend", port)
    allow("alb", 80, "vpclink")
    for port in (8081, 8082, 8083, 8084, 8086):
        allow("core", port, "alb")
        allow("core", port, "support")
    for port in (8080, 8085, 8087, 8089, 8090):
        allow("support", port, "alb")
    allow("support", 8087, "core")
    for source in ("core", "support"):
        for port in (5432, 5672, 5673):
            allow("infra", port, source)
        for port in (9092, 9093, 9094):
            allow("streaming", port, source)
    for port in (15672, 15673):
        allow("infra", port, "frontend")
    allow("streaming", 8088, "frontend")

    ami = session.client("ssm").get_parameter(
        Name="/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64")["Parameter"]["Value"]
    user_data = (ROOT / "deploy/ec2-user-data.sh").read_text(encoding="utf-8")
    instances = state.setdefault("instances", {})
    for role in NODES:
        existing = ec2.describe_instances(Filters=[
            {"Name": "tag:Project", "Values": [PROJECT]},
            {"Name": "tag:Name", "Values": [f"p360-ep6-{role}"]},
            {"Name": "instance-state-name", "Values": ["pending", "running", "stopping", "stopped"]}])
        found = [i for reservation in existing["Reservations"] for i in reservation["Instances"]]
        if len(found) > 1:
            raise RuntimeError(f"Instancias duplicadas para {role}; revisar sin crear otra.")
        if found:
            node = found[0]
        else:
            node = ec2.run_instances(
                ImageId=ami, InstanceType="t3.large", MinCount=1, MaxCount=1,
                ClientToken=f"{PROJECT}-{state['account']}-{role}",
                IamInstanceProfile={"Name": "LabInstanceProfile"},
                NetworkInterfaces=[{"DeviceIndex": 0, "SubnetId": subnets[0]["SubnetId"],
                                    "Groups": [groups[role]], "AssociatePublicIpAddress": True}],
                MetadataOptions={"HttpTokens": "required", "HttpEndpoint": "enabled"},
                BlockDeviceMappings=[{"DeviceName": "/dev/xvda", "Ebs": {
                    "VolumeSize": 40, "VolumeType": "gp3", "DeleteOnTermination": True}}],
                TagSpecifications=[{"ResourceType": "instance", "Tags": tags(f"p360-ep6-{role}")},
                                   {"ResourceType": "volume", "Tags": tags(f"p360-ep6-{role}")}],
                UserData=user_data)["Instances"][0]
        instances[role] = {"id": node["InstanceId"], "private": node.get("PrivateIpAddress"),
                           "public": node.get("PublicIpAddress"), "state": node["State"]["Name"]}
        save(state)
        print(role, node["InstanceId"], node["State"]["Name"], flush=True)


def refresh(session, state):
    ec2 = session.client("ec2")
    for role, entry in state.get("instances", {}).items():
        node = ec2.describe_instances(InstanceIds=[entry["id"]])["Reservations"][0]["Instances"][0]
        entry.update(private=node.get("PrivateIpAddress"), public=node.get("PublicIpAddress"),
                     state=node["State"]["Name"])
    save(state)
    print(json.dumps(state.get("instances", {}), indent=2), flush=True)
    info = session.client("ssm").describe_instance_information()["InstanceInformationList"]
    ours = {entry["id"] for entry in state.get("instances", {}).values()}
    print("SSM:", [(item["InstanceId"], item["PingStatus"]) for item in info if item["InstanceId"] in ours])


def private_settings():
    STATE_DIR.mkdir(exist_ok=True)
    path = STATE_DIR / "secrets.json"
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    values = {key: secrets.token_urlsafe(32) for key in ("db", "rabbit", "cookie", "pepper", "panel")}
    path.write_text(json.dumps(values), encoding="utf-8")
    return values


def send(session, state, role, label, script):
    commands = state.setdefault("commands", {})
    previous = commands.get(label)
    ssm = session.client("ssm")
    if previous:
        result = ssm.get_command_invocation(CommandId=previous["id"], InstanceId=previous["instance"])
        if result["Status"] in ("Pending", "InProgress", "Delayed", "Success"):
            print(label, result["Status"], previous["id"], flush=True)
            return
    response = ssm.send_command(
        InstanceIds=[state["instances"][role]["id"]], DocumentName="AWS-RunShellScript",
        TimeoutSeconds=600, Comment=f"{PROJECT} {label}",
        Parameters={"commands": [script], "executionTimeout": ["3600"]})
    commands[label] = {"id": response["Command"]["CommandId"], "instance": state["instances"][role]["id"]}
    save(state)
    print(label, commands[label]["id"], flush=True)


def configure(session, state):
    refresh(session, state)
    ec2 = session.client("ec2")
    if not state.get("eip"):
        addresses = ec2.describe_addresses(Filters=[{"Name": "tag:Project", "Values": [PROJECT]}])["Addresses"]
        if addresses:
            address = addresses[0]
        else:
            address = ec2.allocate_address(Domain="vpc", TagSpecifications=[
                {"ResourceType": "elastic-ip", "Tags": tags("p360-ep6-frontend")}])
        state["eip"] = {"id": address["AllocationId"], "ip": address["PublicIp"]}
        save(state)
    ec2.associate_address(InstanceId=state["instances"]["frontend"]["id"],
                          AllocationId=state["eip"]["id"])
    state["domain"] = state["eip"]["ip"].replace(".", "-") + ".sslip.io"
    save(state)
    values = private_settings()
    infra = state["instances"]["infra"]["private"]
    streaming = state["instances"]["streaming"]["private"]
    core = state["instances"]["core"]["private"]
    support = state["instances"]["support"]["private"]
    common = {
        "AZURE_TENANT_ID": "e5372bf0-c5e3-4286-887c-79069f209c1f",
        "AZURE_API_AUDIENCE": "7d7e6f82-35dc-4fd2-b580-7776c558d963",
        "CORS_ALLOWED_ORIGINS": "https://" + state["domain"],
        "DB_HOST": infra, "DB_PORT": "5432", "DB_NAME": "pedidos360", "DB_USER": "pedidos360",
        "DB_PASSWORD": values["db"], "RABBITMQ_USER": "pedidos360", "RABBITMQ_PASSWORD": values["rabbit"],
        "RABBITMQ_ADDRESSES": f"{infra}:5672,{infra}:5673",
        "KAFKA_BOOTSTRAP_SERVERS": f"{streaming}:9092,{streaming}:9093,{streaming}:9094",
        "PAYMENT_FINGERPRINT_PEPPER": values["pepper"], "SMTP_ENABLED": "false",
        "PAGOS_URL": f"http://{support}:8087",
        "PRODUCTOS_URL": f"http://{core}:8081", "CLIENTES_URL": f"http://{core}:8082",
        "PEDIDOS_URL": f"http://{core}:8083", "NOTIFICATIONS_URL": f"http://{core}:8084",
        "AUDITORIA_URL": f"http://{core}:8086", "RABBIT_ADMIN_URL": "http://rabbit-admin-service:8085",
    }
    configs = {
        "infra": {"DB_NAME": "pedidos360", "DB_USER": "pedidos360", "DB_PASSWORD": values["db"],
                  "RABBITMQ_USER": "pedidos360", "RABBITMQ_PASSWORD": values["rabbit"],
                  "RABBITMQ_ERLANG_COOKIE": values["cookie"], "MGMT_BIND_ADDRESS": infra},
        "streaming": {"KAFKA_EXTERNAL_HOST": streaming, "MGMT_BIND_ADDRESS": streaming},
        "core": common, "support": common,
    }
    for role, config in configs.items():
        encoded = base64.b64encode("".join(f"{k}={v}\n" for k, v in config.items()).encode()).decode()
        script = f"""#!/bin/bash
set -eu
cloud-init status --wait
docker version >/dev/null
mkdir -p /opt/pedidos360
if [ ! -d /opt/pedidos360/backend/.git ]; then
  git clone https://github.com/Antonellacuvertino/pedidos360-backend.git /opt/pedidos360/backend
fi
cd /opt/pedidos360/backend
git pull --ff-only
umask 077
printf '%s' '{encoded}' | base64 -d > deploy/.env.node
"""
        if role in ("core", "support"):
            selected = " ".join(CORE if role == "core" else SUPPORT)
            script += f"""
docker build -f deploy/Dockerfile.backend -t pedidos360/backend:ep6 . > /opt/pedidos360/build.log 2>&1 || {{ tail -n 50 /opt/pedidos360/build.log; exit 1; }}
docker compose -p pedidos360-backend --env-file deploy/.env.node -f deploy/compose.backend.yml up -d {selected}
docker compose -p pedidos360-backend --env-file deploy/.env.node -f deploy/compose.backend.yml ps
"""
        else:
            file = "compose.infrastructure.yml" if role == "infra" else "compose.kafka.yml"
            project = "pedidos360-base" if role == "infra" else "pedidos360-ep6"
            script += f"docker compose -p {project} --env-file deploy/.env.node -f deploy/{file} up -d\n"
            script += f"docker compose -p {project} --env-file deploy/.env.node -f deploy/{file} ps\n"
        send(session, state, role, f"configure-{role}", script)


def command_status(session, state):
    ssm = session.client("ssm")
    for label, command in state.get("commands", {}).items():
        result = ssm.get_command_invocation(CommandId=command["id"], InstanceId=command["instance"])
        print(label, result["Status"], flush=True)
        print(result.get("StandardOutputContent", "")[-5000:], flush=True)
        print(result.get("StandardErrorContent", "")[-2000:], flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--account", required=True)
    parser.add_argument("--region", default="us-east-1")
    parser.add_argument("action", choices=["provision", "status", "configure", "commands"])
    args = parser.parse_args()
    session = boto3.Session(region_name=args.region)
    account = session.client("sts").get_caller_identity()["Account"]
    if account != args.account:
        raise SystemExit("La cuenta activa no coincide; no se modifico nada.")
    path = STATE_DIR / "state.json"
    state = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"account": account}
    if state["account"] != account:
        raise SystemExit("El estado local pertenece a otra cuenta; no reutilizarlo.")
    if args.action == "provision":
        provision(session, state)
    elif args.action == "status":
        refresh(session, state)
    elif args.action == "configure":
        configure(session, state)
    else:
        command_status(session, state)


if __name__ == "__main__":
    main()
