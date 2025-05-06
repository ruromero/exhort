# AI Model Cards

## Container image

The changes are built and push to `quay.io/ruben/exhort:model-cards`

## Deployment

Use `podman compose` to start the service. Bear in mind that the `.env` file must exist and contain the following format:

```env

AWS_ACCESS_KEY_ID=<aws-access-key-id>
AWS_SECRET_ACCESS_KEY=<aws-secret-access-key>
AWS_REGION=<aws-region> # example: eu-west-1
```

Now start the service with the following command:

```bash
podman compose -f podman-compose.yml --env-file=.env up
```
