"""Documentation-only checks. Run with PyYAML 6.0.3 / openapi-spec-validator 0.7.2.

Does not call the application or claim its planned endpoints are implemented.
"""
from pathlib import Path
import json
import re

import yaml
from openapi_spec_validator import validate_spec
from openapi_schema_validator import OAS30Validator


ROOT = Path(__file__).resolve().parents[2]


class UniqueKeyLoader(yaml.SafeLoader):
    pass


def unique_mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise ValueError(f"Duplicate YAML key: {key}")
        result[key] = loader.construct_object(value_node, deep=deep)
    return result


UniqueKeyLoader.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping
)
spec = yaml.load((ROOT / 'docs/api/phase1-contract.yaml').read_text(encoding='utf-8'), Loader=UniqueKeyLoader)
validate_spec(spec)
plan = (ROOT / 'docs/PROMPTS_PHA_1_4_NGUOI.md').read_text(encoding='utf-8')
md = (ROOT / 'docs/api/phase1-contract.md').read_text(encoding='utf-8')
expected = {eid: (method.lower(), path) for eid, method, path in
            re.findall(r'\| (E\d{2}) \| (GET|POST|PATCH|DELETE) ([^ ]+) \|', plan)}
operations = {op['operationId']: (method, path, op)
              for path, methods in spec['paths'].items()
              for method, op in methods.items()}
assert len(expected) == len(operations) == 15
assert set(operations) == set(expected)
screens = set(re.findall(r'\| (S\d{2}) \|', plan))
assert screens == {f'S{i:02}' for i in range(1, 7)}
consumed = set()
for eid, (method, path, op) in operations.items():
    assert (method, path) == expected[eid], eid
    assert op['x-owner'] in {f'Person {i}' for i in range(1, 5)}, eid
    assert op['x-consumers'] and set(op['x-consumers']) <= screens, eid
    assert op['x-roles'] and op['x-status'] == 'planned', eid
    consumed.update(op['x-consumers'])
    assert f'| {eid} | {method.upper()} {path} |' in md, eid
    if eid == 'E01':
        assert op['security'] == []
    else:
        assert op.get('security', spec['security']) == [{'bearerAuth': []}], eid
    for consumer in op['x-consumers']:
        screen_row = next(line for line in md.splitlines() if line.startswith('| '+consumer+' `'))
        assert eid in screen_row, (eid, consumer)
assert consumed == screens
assert 'reportId' in {p['name'] for p in operations['E14'][2]['parameters']}
assert set(operations['E11'][2]['responses']['204']) == {'description', 'headers'}

# Resolve local references for validating examples against the OAS 3.0 schema dialect.
def resolve(value):
    if isinstance(value, dict):
        if '$ref' in value:
            target = spec
            for part in value['$ref'].removeprefix('#/').split('/'):
                target = target[part]
            return resolve(target)
        return {k: resolve(v) for k, v in value.items()}
    if isinstance(value, list):
        return [resolve(v) for v in value]
    return value


checked = 0
def check(schema, example):
    global checked
    OAS30Validator(resolve(schema), format_checker=OAS30Validator.FORMAT_CHECKER).validate(example)
    checked += 1


for schema in spec['components']['schemas'].values():
    if 'example' in schema:
        check(schema, schema['example'])
for _, _, op in operations.values():
    for message in [op.get('requestBody', {}), *op['responses'].values()]:
        for media in message.get('content', {}).values():
            if 'example' in media:
                check(media['schema'], media['example'])
for name, raw in re.findall(r'### (\w+)\s+```json\s+(.*?)\s+```', md, re.S):
    check(spec['components']['schemas'][name], json.loads(raw))
print(f'PASS: OpenAPI 3.0.3; 15 exact operations from plan; 6 screens; owners/consumers; {checked} schema/media/Markdown examples.')
