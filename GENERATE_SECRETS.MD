# private key PKCS#8 PEM
```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
```

# public key PEM
```bash
openssl pkey -in jwt-private.pem -pubout -out jwt-public.pem
```

# DER to binary
```bash
openssl pkcs8 -topk8 -inform PEM -outform DER -nocrypt -in jwt-private.pem -out jwt-private.der
openssl pkey  -pubin -inform PEM -outform DER -in jwt-public.pem -out jwt-public.der
```

# base64 in one line
```bash
base64 -w 0 jwt-private.der > jwt-private.der.b64
base64 -w 0 jwt-public.der  > jwt-public.der.b64
```