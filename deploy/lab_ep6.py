"""Provision de cinco EC2 del laboratorio autorizado. Requiere boto3.

Lee credenciales del entorno AWS. Estado privado en .deploy-state, fuera de Git.
No elimina recursos. Ejecutar --account con el ID del laboratorio nuevo.
"""
import argparse
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


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--account", required=True)
    parser.add_argument("--region", default="us-east-1")
    parser.add_argument("action", choices=["provision", "status"])
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
    else:
        refresh(session, state)


if __name__ == "__main__":
    main()
