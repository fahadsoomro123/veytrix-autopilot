# Migration Security Review

## Security-sensitive source components

| Component | Risk | Treatment |
|---|---|---|
| `build-nexusnova-autopilot-apk.yml` | Reads NexusNova signing keystore/password/alias secrets and writes a release keystore file | Classification E; not copied directly |
| `MainActivity.java` | Handles a GitHub token | Encryption behavior retained; hardcoded NexusNova repository identity removed |
| Autopilot workflow permissions | Can write contents and rerun Actions | Retained only where required by orchestration; target token is supplied through GitHub secrets |
| Git remote push logic | Could expose credentials if URL/logging is mishandled | Credential persistence disabled; token never printed; push is bounded |
| Repair prompts/logs | Could accidentally leak secret material | Explicit no-secret rules; logs treated as untrusted evidence |

## Invariants

- No credential, signing key, keystore, token, or password is committed to Veytrix.
- `actions/checkout` uses `persist-credentials: false` where applicable.
- The Veytrix workflow does not copy or read NexusNova signing secrets.
- NexusNova package identity is not embedded in the Veytrix core.
- Unknown failures are not automatically rerun.
- Repair and verification handoffs are bounded.
