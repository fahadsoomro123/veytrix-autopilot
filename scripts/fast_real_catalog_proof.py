#!/usr/bin/env python3
import hashlib
import json
import os
import re
import time
from collections import Counter

import requests

HF_API = 'https://huggingface.co/api/models'
TARGET = 3_000_000
TIMEOUT = 30
MAX_RETRIES = 8

session = requests.Session()
if os.environ.get('HF_TOKEN'):
    session.headers['Authorization'] = f"Bearer {os.environ['HF_TOKEN']}"


def get_with_retry(url, params=None):
    for attempt in range(MAX_RETRIES):
        try:
            resp = session.get(url, params=params, timeout=TIMEOUT)
            if resp.status_code == 429:
                wait = int(resp.headers.get('Retry-After', '10'))
                time.sleep(min(wait, 120))
                continue
            resp.raise_for_status()
            return resp
        except requests.RequestException:
            if attempt == MAX_RETRIES - 1:
                raise
            time.sleep(min(2 ** attempt, 30))
    raise RuntimeError('unreachable')


def scan_hf(params):
    url = HF_API
    first = True
    count = 0
    seen = set()
    digest = hashlib.sha256()
    pages = 0
    started = time.time()
    while url:
        resp = get_with_retry(url, params=params if first else None)
        first = False
        pages += 1
        items = resp.json()
        if not isinstance(items, list):
            raise RuntimeError(f'unexpected HF response on page {pages}')
        for item in items:
            model_id = item.get('id') if isinstance(item, dict) else None
            if not model_id:
                continue
            count += 1
            if model_id not in seen:
                seen.add(model_id)
                digest.update(model_id.encode('utf-8'))
                digest.update(b'\n')
        if pages % 100 == 0:
            print(f'hf_pages={pages} hf_models={count} elapsed_sec={time.time()-started:.1f}', flush=True)
        nxt = resp.links.get('next')
        url = nxt['url'] if nxt else None
    return count, len(seen), digest.hexdigest(), pages, time.time() - started


report = {
    'target_models': TARGET,
    'sources': {},
}
print('FAST_REAL_CATALOG_PROOF=START', flush=True)

# Exhaustive HF public model catalog scan using direct cursor pagination with
# the API's maximum page size (1000). This is real catalog data, not synthetic.
count, unique, digest, pages, elapsed = scan_hf({'limit': 1000})
report['sources']['huggingface'] = {
    'discovered': count,
    'unique': unique,
    'pages': pages,
    'catalog_sha256': digest,
    'elapsed_sec': round(elapsed, 2),
}
print(f'HF_DISCOVERED={count}', flush=True)
print(f'HF_UNIQUE={unique}', flush=True)
print(f'HF_PAGES={pages}', flush=True)
print(f'HF_CATALOG_SHA256={digest}', flush=True)

# Provider-backed catalog. HF documents inference_provider=all as models served
# by at least one inference provider; mapping status is live/staging.
def scan_provider_catalog():
    url = HF_API
    first = True
    models = 0
    unique_ids = set()
    live_mappings = 0
    provider_counts = Counter()
    provider_models = []
    pages = 0
    while url:
        params = {'inference_provider': 'all', 'limit': 1000,
                  'expand[]': ['pipeline_tag', 'inferenceProviderMapping']} if first else None
        resp = get_with_retry(url, params=params)
        first = False
        pages += 1
        items = resp.json()
        for item in items:
            if not isinstance(item, dict) or not item.get('id'):
                continue
            models += 1
            unique_ids.add(item['id'])
            provider_models.append(item)
            mapping = item.get('inferenceProviderMapping') or item.get('inference_provider_mapping') or {}
            if isinstance(mapping, dict):
                for provider, info in mapping.items():
                    provider_counts[provider] += 1
                    status = info.get('status') if isinstance(info, dict) else getattr(info, 'status', None)
                    if status == 'live':
                        live_mappings += 1
        nxt = resp.links.get('next')
        url = nxt['url'] if nxt else None
    return models, len(unique_ids), live_mappings, dict(provider_counts), provider_models, pages

pm, pu, live, providers, provider_models, provider_pages = scan_provider_catalog()
report['sources']['huggingface_provider_catalog'] = {
    'provider_backed_models': pm,
    'unique_models': pu,
    'live_provider_mappings': live,
    'provider_counts': providers,
    'pages': provider_pages,
}
print(f'HF_PROVIDER_BACKED={pm}', flush=True)
print(f'HF_PROVIDER_UNIQUE={pu}', flush=True)
print(f'HF_LIVE_PROVIDER_MAPPINGS={live}', flush=True)
print(f'HF_PROVIDER_TYPES={len(providers)}', flush=True)

# Public OpenRouter catalog probe (metadata only unless an API key is configured).
try:
    r = get_with_retry('https://openrouter.ai/api/v1/models')
    payload = r.json()
    models = payload.get('data') if isinstance(payload, dict) else None
    if isinstance(models, list):
        free = [m for m in models if isinstance(m, dict) and m.get('pricing', {}).get('prompt') in ('0', 0) and m.get('pricing', {}).get('completion') in ('0', 0)]
        report['sources']['openrouter'] = {'models': len(models), 'free_models': len(free)}
        print(f'OPENROUTER_MODELS={len(models)}', flush=True)
        print(f'OPENROUTER_FREE_MODELS={len(free)}', flush=True)
except Exception as exc:
    report['sources']['openrouter'] = {'status': 'ERROR', 'error': str(exc)[:300]}

# Puter public model-details endpoint probe. This endpoint is the backing source
# documented for puter.ai.listModels().
try:
    r = get_with_retry('https://api.puter.com/puterai/chat/models/details')
    payload = r.json()
    models = payload.get('models') if isinstance(payload, dict) else payload
    if isinstance(models, list):
        report['sources']['puter'] = {'models': len(models)}
        print(f'PUTER_MODELS={len(models)}', flush=True)
except Exception as exc:
    report['sources']['puter'] = {'status': 'ERROR', 'error': str(exc)[:300]}

# Real inference probe only when explicitly authenticated. This never claims
# metadata status is the same thing as a successful model call.
token = os.environ.get('HF_TOKEN')
report['live_inference_probe'] = {'status': 'SKIPPED_NO_HF_TOKEN', 'tested': 0, 'passed': 0, 'failed': 0}
if token:
    from huggingface_hub import InferenceClient
    client = InferenceClient(token=token, provider='auto')
    candidates = []
    for model in provider_models:
        if len(candidates) >= 5:
            break
        mapping = model.get('inferenceProviderMapping') or model.get('inference_provider_mapping') or {}
        if any((info.get('status') == 'live' and info.get('task') == 'conversational') for info in mapping.values() if isinstance(info, dict)):
            candidates.append(model['id'])
    report['live_inference_probe']['status'] = 'EXECUTED'
    for model_id in candidates:
        try:
            response = client.chat.completions.create(
                model=model_id,
                messages=[{'role': 'user', 'content': 'Reply with exactly VEYTRIX_OK'}],
                max_tokens=8,
            )
            report['live_inference_probe']['tested'] += 1
            if str(response.choices[0].message.content or '').strip() == 'VEYTRIX_OK':
                report['live_inference_probe']['passed'] += 1
            else:
                report['live_inference_probe']['failed'] += 1
        except Exception:
            report['live_inference_probe']['tested'] += 1
            report['live_inference_probe']['failed'] += 1

report['threshold_reached'] = unique >= TARGET
report['proof_timestamp_utc'] = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())

with open('fast-real-catalog-proof.json', 'w', encoding='utf-8') as fh:
    json.dump(report, fh, indent=2, sort_keys=True)

with open('fast-real-catalog-proof.txt', 'w', encoding='utf-8') as fh:
    fh.write(f"TARGET={TARGET}\n")
    fh.write(f"HF_DISCOVERED={count}\n")
    fh.write(f"HF_UNIQUE={unique}\n")
    fh.write(f"HF_PAGES={pages}\n")
    fh.write(f"HF_CATALOG_SHA256={digest}\n")
    fh.write(f"HF_PROVIDER_BACKED={pm}\n")
    fh.write(f"HF_PROVIDER_UNIQUE={pu}\n")
    fh.write(f"HF_LIVE_PROVIDER_MAPPINGS={live}\n")
    fh.write(f"THRESHOLD_REACHED={report['threshold_reached']}\n")
    fh.write(f"LIVE_INFERENCE_PROBE_STATUS={report['live_inference_probe']['status']}\n")
    fh.write(f"LIVE_INFERENCE_TESTED={report['live_inference_probe']['tested']}\n")
    fh.write(f"LIVE_INFERENCE_PASSED={report['live_inference_probe']['passed']}\n")
    fh.write(f"LIVE_INFERENCE_FAILED={report['live_inference_probe']['failed']}\n")
    for source, data in report['sources'].items():
        fh.write(f"{source.upper()}={json.dumps(data, sort_keys=True)}\n")

print('FAST_REAL_CATALOG_PROOF=PASS' if report['threshold_reached'] else 'FAST_REAL_CATALOG_PROOF=FAIL_THRESHOLD', flush=True)
