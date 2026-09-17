#!/usr/bin/env python3
import hashlib
import json
import os
import sys
import time

from huggingface_hub import HfApi, InferenceClient

TARGET = 3_000_000
api = HfApi(token=os.environ.get('HF_TOKEN') or False)

report = {
    'target_models': TARGET,
    'hf_discovered_models': 0,
    'hf_unique_models': 0,
    'hf_warm_models': 0,
    'hf_provider_backed_models': 0,
    'hf_live_provider_mappings': 0,
    'provider_counts': {},
    'live_probe': {
        'status': 'SKIPPED_NO_HF_TOKEN',
        'tested': 0,
        'passed': 0,
        'failed': 0,
        'samples': [],
    },
}
seen = set()
sha = hashlib.sha256()
start = time.time()

print('REAL_3M_MODEL_PROOF=START', flush=True)
print('Scanning Hugging Face Hub model catalog with pagination...', flush=True)

# Exhaustive public catalog discovery. HfApi.list_models() is an iterator over
# the Hub catalog and handles pagination/rate-limit retries.
for model in api.list_models(limit=None):
    model_id = getattr(model, 'id', None)
    if not model_id:
        continue
    report['hf_discovered_models'] += 1
    if model_id not in seen:
        seen.add(model_id)
        sha.update(model_id.encode('utf-8'))
        sha.update(b'\n')
    if report['hf_discovered_models'] % 100_000 == 0:
        elapsed = time.time() - start
        print(
            f'progress_discovered={report["hf_discovered_models"]} '
            f'unique={len(seen)} elapsed_sec={elapsed:.1f}',
            flush=True,
        )

report['hf_unique_models'] = len(seen)
report['catalog_sha256'] = sha.hexdigest()
print(f'hf_discovered_models={report["hf_discovered_models"]}', flush=True)
print(f'hf_unique_models={report["hf_unique_models"]}', flush=True)
print(f'catalog_sha256={report["catalog_sha256"]}', flush=True)

# Server-side warm catalog: only models currently served by at least one
# inference provider.
print('Scanning warm/provider-backed catalog...', flush=True)
for _ in api.list_models(inference='warm', limit=None):
    report['hf_warm_models'] += 1
print(f'hf_warm_models={report["hf_warm_models"]}', flush=True)

# Provider-backed metadata with live/staging mapping information.
print('Scanning provider mappings...', flush=True)
provider_models = []
for model in api.list_models(
    inference_provider='all',
    expand=['pipeline_tag', 'inferenceProviderMapping'],
    limit=None,
):
    model_id = getattr(model, 'id', None)
    if not model_id:
        continue
    report['hf_provider_backed_models'] += 1
    provider_models.append(model)
    mapping = getattr(model, 'inference_provider_mapping', None) or {}
    if isinstance(mapping, dict):
        for provider, info in mapping.items():
            status = getattr(info, 'status', None) if not isinstance(info, dict) else info.get('status')
            report['provider_counts'][provider] = report['provider_counts'].get(provider, 0) + 1
            if status == 'live':
                report['hf_live_provider_mappings'] += 1

print(f'hf_provider_backed_models={report["hf_provider_backed_models"]}', flush=True)
print(f'hf_live_provider_mappings={report["hf_live_provider_mappings"]}', flush=True)

# Optional real inference probe. The workflow supplies HF_TOKEN only when
# an authenticated token is configured in GitHub Actions secrets.
token = os.environ.get('HF_TOKEN')
if token:
    client = InferenceClient(token=token, provider='auto')
    candidates = []
    for model in provider_models:
        if len(candidates) >= 10:
            break
        mapping = getattr(model, 'inference_provider_mapping', None) or {}
        live_chat = False
        if isinstance(mapping, dict):
            for info in mapping.values():
                task = getattr(info, 'task', None) if not isinstance(info, dict) else info.get('task')
                status = getattr(info, 'status', None) if not isinstance(info, dict) else info.get('status')
                if status == 'live' and task == 'conversational':
                    live_chat = True
                    break
        if live_chat:
            candidates.append(model.id)

    report['live_probe']['status'] = 'EXECUTED'
    for model_id in candidates:
        item = {'model': model_id}
        t0 = time.time()
        try:
            response = client.chat.completions.create(
                model=model_id,
                messages=[{'role': 'user', 'content': 'Reply with exactly VEYTRIX_OK'}],
                max_tokens=8,
            )
            text = str(response.choices[0].message.content or '').strip()
            item['latency_ms'] = round((time.time() - t0) * 1000, 1)
            item['response_valid'] = text == 'VEYTRIX_OK'
            if item['response_valid']:
                item['status'] = 'PASS'
                report['live_probe']['passed'] += 1
            else:
                item['status'] = 'FAIL_INVALID_RESPONSE'
                report['live_probe']['failed'] += 1
        except Exception as exc:
            item['status'] = 'FAIL_EXCEPTION'
            item['error'] = str(exc)[:500]
            report['live_probe']['failed'] += 1
        report['live_probe']['samples'].append(item)
    report['live_probe']['tested'] = len(candidates)

report['elapsed_sec'] = round(time.time() - start, 2)
report['three_million_threshold_reached'] = report['hf_unique_models'] >= TARGET

with open('real-3m-model-proof.json', 'w', encoding='utf-8') as fh:
    json.dump(report, fh, indent=2, sort_keys=True)

with open('real-3m-model-proof.txt', 'w', encoding='utf-8') as fh:
    fh.write(f"TARGET={TARGET}\n")
    fh.write(f"HF_DISCOVERED={report['hf_discovered_models']}\n")
    fh.write(f"HF_UNIQUE={report['hf_unique_models']}\n")
    fh.write(f"HF_WARM={report['hf_warm_models']}\n")
    fh.write(f"HF_PROVIDER_BACKED={report['hf_provider_backed_models']}\n")
    fh.write(f"HF_LIVE_PROVIDER_MAPPINGS={report['hf_live_provider_mappings']}\n")
    fh.write(f"THREE_MILLION_THRESHOLD_REACHED={report['three_million_threshold_reached']}\n")
    fh.write(f"CATALOG_SHA256={report['catalog_sha256']}\n")
    fh.write(f"ELAPSED_SEC={report['elapsed_sec']}\n")
    fh.write(f"LIVE_PROBE_STATUS={report['live_probe']['status']}\n")
    fh.write(f"LIVE_PROBE_TESTED={report['live_probe']['tested']}\n")
    fh.write(f"LIVE_PROBE_PASSED={report['live_probe']['passed']}\n")
    fh.write(f"LIVE_PROBE_FAILED={report['live_probe']['failed']}\n")

if not report['three_million_threshold_reached']:
    print('REAL_3M_MODEL_PROOF=FAIL_THRESHOLD_NOT_REACHED', flush=True)
    sys.exit(2)

print('REAL_3M_MODEL_PROOF=PASS', flush=True)
