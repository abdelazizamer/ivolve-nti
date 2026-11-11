# Labs 27 and 28: MySQL with Ansible Vault and AWS Dynamic Inventory

## Objective

This combined project completes two tasks:

- **Lab 27:** Configure MySQL with an Ansible role, create the `ivolve` database, create `ivolveuser`, and protect its password with Ansible Vault.
- **Lab 28:** Use the AWS EC2 dynamic inventory plugin to discover running instances tagged `server=ivolve` and run the MySQL playbook on them.

## AWS setup

1. Tag the managed EC2 instance:

   ```text
   Key:   server
   Value: ivolve
   ```

2. Attach an IAM role to the Ansible Control Node. The role needs permission to read EC2 instances, such as `AmazonEC2ReadOnlyAccess`.

3. Confirm that the Control Node can reach the managed instance through SSH using its private IP address.

## Project structure

```text
lab27-28-ansible-vault-dynamic-inventory/
├── inventory/aws_ec2.yml
├── group_vars/ivolve_servers/main.yml
├── group_vars/ivolve_servers/vault.yml.example
├── roles/mysql/tasks/main.yml
├── site.yml
├── validate.yml
├── requirements.yml
└── .gitignore
```

## Control Node prerequisites

Install the AWS Python SDK and Ansible collections:

```bash
sudo apt update
sudo apt install -y python3-boto3
ansible-galaxy collection install -r requirements.yml
```

## Create the encrypted Vault file

The project intentionally excludes the real `vault.yml` from upload because it contains a database password.

Copy the template and replace the placeholder with the database password:

```bash
cp group_vars/ivolve_servers/vault.yml.example group_vars/ivolve_servers/vault.yml
nano group_vars/ivolve_servers/vault.yml
```

Encrypt the file. Choose a Vault password and keep it private:

```bash
ansible-vault encrypt --vault-id lab27@prompt group_vars/ivolve_servers/vault.yml
```

## Verify AWS Dynamic Inventory

```bash
ansible-inventory -i inventory/aws_ec2.yml --graph
```

Expected group and host format:

```text
@ivolve_servers:
  |--172.31.26.136
```

## Run the MySQL role

```bash
ansible-playbook -i inventory/aws_ec2.yml site.yml --vault-id lab27@prompt
```

The role installs MySQL and PyMySQL, starts MySQL, creates the `ivolve` database, and creates `ivolveuser@localhost` with all privileges on `ivolve.*`.

The role uses `caching_sha2_password`, which is compatible with current MySQL versions where `mysql_native_password` is unavailable.

## Validate database access

```bash
ansible-playbook -i inventory/aws_ec2.yml validate.yml --vault-id lab27@prompt
```

Expected database output includes:

```text
Database
information_schema
ivolve
performance_schema
```
# Labs 27 and 28: MySQL with Ansible Vault and AWS Dynamic Inventory

## Objective

This combined project completes two tasks:

- **Lab 27:** Configure MySQL with an Ansible role, create the `ivolve` database, create `ivolveuser`, and protect its password with Ansible Vault.
- **Lab 28:** Use the AWS EC2 dynamic inventory plugin to discover running instances tagged `server=ivolve` and run the MySQL playbook on them.

## AWS setup

1. Tag the managed EC2 instance:

   ```text
   Key:   server
   Value: ivolve
   ```

2. Attach an IAM role to the Ansible Control Node. The role needs permission to read EC2 instances, such as `AmazonEC2ReadOnlyAccess`.

3. Confirm that the Control Node can reach the managed instance through SSH using its private IP address.

## Project structure

```text
lab27-28-ansible-vault-dynamic-inventory/
├── inventory/aws_ec2.yml
├── group_vars/ivolve_servers/main.yml
├── group_vars/ivolve_servers/vault.yml.example
├── roles/mysql/tasks/main.yml
├── site.yml
├── validate.yml
├── requirements.yml
└── .gitignore
```

## Control Node prerequisites

Install the AWS Python SDK and Ansible collections:

```bash
sudo apt update
sudo apt install -y python3-boto3
ansible-galaxy collection install -r requirements.yml
```

## Create the encrypted Vault file

The project intentionally excludes the real `vault.yml` from upload because it contains a database password.

Copy the template and replace the placeholder with the database password:

```bash
cp group_vars/ivolve_servers/vault.yml.example group_vars/ivolve_servers/vault.yml
nano group_vars/ivolve_servers/vault.yml
```

Encrypt the file. Choose a Vault password and keep it private:

```bash
ansible-vault encrypt --vault-id lab27@prompt group_vars/ivolve_servers/vault.yml
```

## Verify AWS Dynamic Inventory

```bash
ansible-inventory -i inventory/aws_ec2.yml --graph
```

Expected group and host format:

```text
@ivolve_servers:
  |--172.31.26.136
```

## Run the MySQL role

```bash
ansible-playbook -i inventory/aws_ec2.yml site.yml --vault-id lab27@prompt
```

The role installs MySQL and PyMySQL, starts MySQL, creates the `ivolve` database, and creates `ivolveuser@localhost` with all privileges on `ivolve.*`.

The role uses `caching_sha2_password`, which is compatible with current MySQL versions where `mysql_native_password` is unavailable.

## Validate database access

```bash
ansible-playbook -i inventory/aws_ec2.yml validate.yml --vault-id lab27@prompt
```

Expected database output includes:

```text
Database
information_schema
ivolve
performance_schema
```

## Evidence screenshots

Capture these three commands and their output:

```bash
ansible-inventory -i inventory/aws_ec2.yml --graph
ansible-playbook -i inventory/aws_ec2.yml site.yml --vault-id lab27@prompt
ansible-playbook -i inventory/aws_ec2.yml validate.yml --vault-id lab27@prompt
```

<img width="1915" height="318" alt="Screenshot 2026-11-11 192537" src="https://github.com/user-attachments/assets/9a0c5289-3703-43ed-bfad-fdc4151ee897" />
<img width="1905" height="839" alt="image" src="https://github.com/user-attachments/assets/c662afd0-3d4b-4af5-99a3-3e80a62a515a" />





The playbook recap should show `failed=0` and `unreachable=0`. Do not include Vault passwords or unencrypted secret content in screenshots.
