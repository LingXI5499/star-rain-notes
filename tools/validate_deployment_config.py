#!/usr/bin/env python3
"""Reject duplicate YAML keys before restarting; report locations, never configuration values."""
import argparse
from pathlib import Path
import sys
import yaml

class UniqueKeysLoader(yaml.SafeLoader):
    pass

def mapping(loader, node, deep=False):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in result:
            raise yaml.constructor.ConstructorError('Duplicate mapping key', node.start_mark,
                    'duplicate key at line ' + str(key_node.start_mark.line + 1), key_node.start_mark)
        result[key] = loader.construct_object(value_node, deep=deep)
    return result

UniqueKeysLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, mapping)

def validate(path):
    try:
        config = yaml.load(Path(path).read_text(encoding='utf-8-sig'), Loader=UniqueKeysLoader)
        if not isinstance(config, dict): raise ValueError('Expected a YAML mapping')
        return True
    except yaml.YAMLError as error:
        mark = getattr(error, 'problem_mark', None)
        location = ' at line ' + str(mark.line + 1) if mark else ''
        print('Invalid YAML: ' + str(path) + location, file=sys.stderr)
    except (OSError, ValueError):
        print('Cannot validate configuration: ' + str(path), file=sys.stderr)
    return False

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('config', nargs='+')
    paths = parser.parse_args().config
    if not all(validate(path) for path in paths): sys.exit(1)
    print('Configuration syntax and unique keys verified.')
