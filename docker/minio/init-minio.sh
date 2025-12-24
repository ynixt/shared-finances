#!/bin/sh
set -e

echo "Running MinIO init script..."
echo "BUCKET=$BUCKET"

mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"
mc mb -p "local/$BUCKET" || true

echo "MinIO init ok (bucket=$BUCKET)"
