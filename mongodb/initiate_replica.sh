#!/bin/bash

echo "Starting replica set initialize"
until mongo --host shared_finances-mongo --eval "print(\"waited for connection\")"
do
    sleep 2
done
echo "Connection finished"
echo "Creating replica set"
mongo --host shared_finances-mongo <<EOF
rs.initiate({
      _id: "rs0",
      version: 1,
      members: [
         { _id: 0, host : "db:27017" }
      ]
   }
)
EOF
echo "replica set created"