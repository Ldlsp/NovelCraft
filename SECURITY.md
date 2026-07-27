# Security Policy

## Reporting a Vulnerability

Do not include credentials, user manuscripts, database files, or proof-of-concept exploit details in a public issue.

After the GitHub repository is published, use its private vulnerability reporting channel. If that channel is not yet enabled, open a minimal public issue asking the maintainer for a private contact method.

## Sensitive Data

- Never commit API keys, Android signing keys, `local.properties`, or exported user data.
- Revoke a key immediately if it is exposed.
- Before publishing a release APK, verify that it is signed with the intended release key rather than the debug key.
