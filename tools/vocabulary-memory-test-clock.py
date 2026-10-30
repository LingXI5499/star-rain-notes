"""Advance one direction's due time in an explicitly named disposable E2E database."""
import os
import re
import sys
from urllib.parse import urlsplit

import pymysql


def mark_due(account_id: int, word_id: int) -> None:
    target = urlsplit(os.environ['STAR_RAIN_E2E_DB_URL'].removeprefix('jdbc:'))
    schema = target.path.removeprefix('/')
    if target.scheme != 'mysql' or target.hostname not in ('127.0.0.1', 'localhost') or not re.fullmatch(r'sr_vocab_flow_[0-9]+', schema):
        raise ValueError('Only a loopback sr_vocab_flow_<digits> disposable database is allowed')
    if account_id <= 0 or word_id <= 0:
        raise ValueError('Account and word IDs must be positive')
    with pymysql.connect(host=target.hostname, port=target.port or 3306, database=schema,
                         user=os.environ['STAR_RAIN_E2E_DB_USERNAME'], password=os.environ['STAR_RAIN_E2E_DB_PASSWORD'],
                         connect_timeout=5, read_timeout=5, write_timeout=5) as connection:
        with connection.cursor() as cursor:
            cursor.execute("""UPDATE sr_english_vocabulary_mode_memory
                SET next_review_at=DATE_SUB(UTC_TIMESTAMP(3),INTERVAL 1 MINUTE)
                WHERE account_id=%s AND word_id=%s AND direction='EN_TO_ZH'""", (account_id, word_id))
            if cursor.rowcount != 1:
                raise ValueError('Expected exactly one previously rated test direction')
        connection.commit()


if __name__ == '__main__':
    mark_due(int(sys.argv[1]), int(sys.argv[2]))
