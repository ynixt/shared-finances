#!/bin/bash

echo "Starting replica set initialize"
until mongo --host shared_finances-mongo --eval "print(\"waited for connection\")"
do
    sleep 2
done
echo "Connection finished"
echo "Creating replica set"
mongo --host shared_finances-mongo <<EOF
rs.initiate()
EOF
echo "replica set created"