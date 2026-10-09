#!/usr/bin/env python3
"""Narrow OSS V1 facade for disposable files backed by Gateway attachments.

Only signed PUT/GET/HEAD/DELETE on allowlisted notebook paths, up to 1 MiB,
and bounded notebook-prefix listing. No multipart or global user backup.
Native replication still belongs entirely to Couchbase Sync Gateway.
"""
import base64
from datetime import datetime, timezone
from email.utils import formatdate, parsedate_to_datetime
import hashlib
import hmac
import json
import re
import os
import xml.etree.ElementTree as ET
import time
from urllib.error import HTTPError
from urllib.parse import unquote, urlsplit, parse_qs
from xml.sax.saxutils import escape

UID = 'powersync-lab'
BUCKET = 'powersync-lab'

def injected_fault(state, method, key, fixture, device, now=None):
    """Read-only, expiring receiver fault; uploads and stored objects survive."""
    if method != 'GET':
        return None
    now = time.time() if now is None else now
    try:
        rule = json.loads((state / 'asset-fault.json').read_text())
    except FileNotFoundError:
        return None
    prefix = UID + '/note/' + fixture + '/resource/data/'
    if (rule.get('fixture') != fixture or rule.get('deviceId') != device['deviceId']
            or rule.get('status') not in (404, 503)
            or not isinstance(rule.get('armedAt'), (int, float))
            or not isinstance(rule.get('expiresAt'), (int, float))
            or not 0 < rule['expiresAt'] - rule['armedAt'] <= 300
            or not rule['armedAt'] <= now < rule['expiresAt']
            or not (key.startswith(prefix) or (rule.get('key') == key and re.fullmatch(UID+r'/resource/[A-Za-z0-9._-]+\.html',key))) or not key.endswith('.html')
            or key.endswith('-history.html')
            or key in rule.get('excludeKeys', [])
            or (rule.get('key') is not None and rule['key'] != key)):
        return None
    return rule['status']

def publication_hold(state, method, key, device, now=None):
    """Exact-device/key PUT acknowledgement hold after a successful durable store."""
    if method != 'PUT':
        return None
    now = time.time() if now is None else now
    try:
        rule = json.loads((state / 'asset-publication-hold.json').read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        return None
    start, end = rule.get('armedAt'), rule.get('expiresAt')
    matches = rule.get('key') == key
    fixture = rule.get('nextNotebookHtml')
    if isinstance(fixture, str) and re.fullmatch('[a-f0-9]{32}|[a-f0-9-]{36}', fixture):
        matches = bool(re.fullmatch(r'powersync-lab/note/' + re.escape(fixture)
                                  + r'/resource/data/[a-f0-9-]{36}\.html', key))
    if (rule.get('mode') != 'hold_put_ack' or not matches
            or rule.get('deviceId') != device['deviceId']
            or not isinstance(start, (int, float)) or not isinstance(end, (int, float))
            or not 0 < end - start <= 60 or not start <= now < end):
        return None
    return end

def canonical(method, path, headers):
    normalized = {k.lower(): v.strip() for k, v in headers.items()}
    selected = {k: v for k, v in normalized.items()
                if k in ('content-md5', 'content-type', 'date') or k.startswith('x-oss-')}
    selected.setdefault('content-md5', '')
    selected.setdefault('content-type', '')
    return method + '\n' + ''.join((k + ':' if k.startswith('x-oss-') else '') + selected[k] + '\n'
                                    for k in sorted(selected)) + path

def transfer_fault(state,method,key,fixture,device,now=None):
    """Exact-key, expiring byte-transfer faults; stored server bytes stay intact."""
    if method!='GET':return None
    now=time.time() if now is None else now
    try:rule=json.loads((state/'asset-transfer-fault.json').read_text())
    except FileNotFoundError:return None
    if (rule.get('key')!=key or rule.get('fixture')!=fixture or rule.get('deviceId')!=device['deviceId']
            or rule.get('mode') not in ('corrupt','truncate','slow')
            or not isinstance(rule.get('armedAt'),(int,float)) or not isinstance(rule.get('expiresAt'),(int,float))
            or not 0<rule['expiresAt']-rule['armedAt']<=300
            or not rule['armedAt']<=now<rule['expiresAt']):return None
    return rule['mode']

def reader_upload_fault(state, method, key, fixture, device, now=None):
    """Bounded owned-reader PUT failure before any backend read or mutation."""
    if method != 'PUT' or not dynamic_reader_key(key, fixture):
        return False
    now = time.time() if now is None else now
    try:
        rule = json.loads((state / 'reader-upload-fault.json').read_text())
    except (FileNotFoundError, json.JSONDecodeError):
        return False
    return (rule.get('mode') == 'put_503' and rule.get('bookId') == fixture
            and rule.get('deviceId') == device['deviceId']
            and isinstance(rule.get('armedAt'), (int, float))
            and isinstance(rule.get('expiresAt'), (int, float))
            and 0 < rule['expiresAt'] - rule['armedAt'] <= 180
            and rule['armedAt'] <= now < rule['expiresAt'])


def reader_keys(book, native_assets=None):
    return (f'{UID}/reader/{book}/book/{book}.epub',
            f'{UID}/reader/{book}/book/{book}.pdf',
            f'{UID}/reader/{book}/thumbnail/{book}.png') + tuple(
                key for key, owner in (native_assets or {}).items() if owner == book)

def dynamic_reader_key(key, book):
    return bool(re.fullmatch(re.escape(f'{UID}/reader/{book}/') +
                             r'(?:point/[A-Za-z0-9#_-]+#points|shape/[A-Za-z0-9#_-]+\.zip)', key))

def reader_key_admitted(key, book, native_assets=None, dynamic_native_books=()):
    return (key in reader_keys(book, native_assets) or
            (book in dynamic_native_books and dynamic_reader_key(key, book)))

def scope(target, ids, v2=None, reader_ids=(), native_assets=None, dynamic_native_books=()):
    if any(book not in reader_ids for book in dynamic_native_books):
        raise ValueError('Dynamic native keys require enrolled books')
    url = urlsplit(target)
    if url.query:
        raise NotImplementedError('Object query operations are outside the lab')
    path = unquote(url.path)
    parts = path.split('/')
    if any(p in ('.', '..') for p in parts) or '\\' in path or '\x00' in path:
        raise ValueError('Invalid object path')
    key='/'.join(parts[2:])
    for book in reader_ids:
        if parts[:2] == ['', BUCKET] and reader_key_admitted(key, book, native_assets, dynamic_native_books):
            return path, key, book
    if (len(parts)==5 and parts[:4]==['',BUCKET,UID,'resource']
            and re.fullmatch(r'[A-Za-z0-9._-]+\.html',parts[4])
            and (v2 or {}).get(key) in ids):
        return path,key,v2[key]
    if len(parts) < 6 or parts[:4] != ['', BUCKET, UID, 'note'] or parts[4] not in ids:
        raise ValueError('Object outside disposable notebook allowlist')
    return path, '/'.join(parts[2:]), parts[4]

def authenticate(method, path, headers, registry):
    header = headers.get('Authorization', '')
    if not header.startswith('OSS ') or ':' not in header:
        raise ValueError('OSS authorization required')
    key, signature = header[4:].split(':', 1)
    for device in registry.values():
        creds = device.get('oss', {})
        if key != creds.get('AccessKeyId'):
            continue
        if device.get('ossExpiresAt', 0) <= time.time():
            raise ValueError('Expired lab OSS credentials')
        if not hmac.compare_digest(headers.get('x-oss-security-token', ''), creds['SecurityToken']):
            raise ValueError('Invalid lab security token')
        signed = canonical(method, path, headers)
        expected = base64.b64encode(hmac.new(creds['AccessKeySecret'].encode(), signed.encode(), hashlib.sha1).digest()).decode()
        if not hmac.compare_digest(signature, expected):
            raise ValueError('Invalid OSS signature')
        date = parsedate_to_datetime(headers.get('Date', ''))
        if abs(time.time() - date.timestamp()) > 900:
            raise ValueError('Stale signed date')
        return device
    raise ValueError('Unknown OSS access key')

def bulk_delete(server,target,headers,body,gateway,ids,v2,operation_id):
    url=urlsplit(target)
    if url.path.rstrip('/')!='/'+BUCKET or parse_qs(url.query,keep_blank_values=True)!={'delete':['']}:
        raise NotImplementedError('Unsupported bulk operation')
    if b'<!' in body or len(body)>1048576:raise ValueError('Invalid delete XML')
    try:root=ET.fromstring(body)
    except ET.ParseError as error:raise ValueError('Invalid delete XML') from error
    if root.tag!='Delete' or any(node.tag not in ('Quiet','Object') for node in root):raise ValueError('Invalid delete XML')
    quiet=root.findtext('Quiet','false')
    if quiet not in ('true','false'):raise ValueError('Invalid delete quiet option')
    nodes=root.findall('Object')
    if not 1<=len(nodes)<=1000:raise ValueError('Invalid delete count')
    keys=[]
    for node in nodes:
        if len(node)!=1 or node[0].tag!='Key' or not node[0].text:raise ValueError('Invalid delete key')
        key=node[0].text
        path,scoped,fixture=scope('/'+BUCKET+'/'+key,ids,v2)
        if scoped!=key:raise ValueError('Noncanonical delete key')
        keys.append((key,fixture))
    with server.lock:device=authenticate('POST','/'+BUCKET+'/?delete',headers,server.registry)
    if getattr(server,'is_revoked',lambda d:False)(device):raise ValueError('Revoked lab device')
    if headers.get('Content-MD5') and not hmac.compare_digest(headers['Content-MD5'],base64.b64encode(hashlib.md5(body).digest()).decode()):raise ValueError('Content MD5 mismatch')
    try:rule=json.loads((gateway.STATE/'bulk-delete-fault.json').read_text())
    except FileNotFoundError:rule={}
    now=time.time()
    if (rule.get('deviceId')==device['deviceId'] and rule.get('key') in dict(keys)
            and isinstance(rule.get('armedAt'),(int,float)) and isinstance(rule.get('expiresAt'),(int,float))
            and 0<rule['expiresAt']-rule['armedAt']<=300 and rule['armedAt']<=now<rule['expiresAt']):
        response=b'<Error><Code>ServiceUnavailable</Code></Error>'
        return device,503,{'Content-Type':'application/xml','X-PowerSync-Injected-Fault':'bulk-delete-503'},response,len(response)
    pending=[]
    for key,fixture in dict(keys).items():
        doc_id='labasset-'+hashlib.sha256(key.encode()).hexdigest();endpoint='/neocloud/'+doc_id
        try:old=gateway.request(endpoint,auth=server.auth)
        except HTTPError as error:
            with error:
                if error.code!=404:raise
            old=None
        if old and (old.get('type')!='labAsset' or old.get('user')!=UID or old.get('ossKey')!=key or old.get('documentUniqueId')!=fixture):raise ValueError('Unexpected deletion target')
        pending.append((key,endpoint,old))
    results=[];xml=[]
    for key,endpoint,old in pending:
        result=dict(at=time.time(),operationId=operation_id,deviceId=device['deviceId'],backendDocId=endpoint.rsplit('/',1)[1],alreadyAbsent=not bool(old))
        try:
            if old:
                saved=gateway.request(endpoint,'PUT',{'_rev':old['_rev'],'_deleted':True},server.auth);result['backendRev']=saved['rev']
            result['deleted']=True
            if quiet=='false':xml.append('<Deleted><Key>'+escape(key)+'</Key></Deleted>')
        except HTTPError:
            result['deleted']=False;xml.append('<Error><Key>'+escape(key)+'</Key><Code>InternalError</Code></Error>')
        results.append(result)
        with server.lock:
            with (gateway.STATE/'bulk-delete.jsonl').open('a') as stream:stream.write(json.dumps(result)+'\n');stream.flush();os.fsync(stream.fileno())
    response=('<?xml version="1.0" encoding="UTF-8"?><DeleteResult>'+''.join(xml)+'</DeleteResult>').encode()
    return device,200,{'Content-Type':'application/xml','x-oss-request-id':'powersync-disposable-lab','X-PowerSync-Deleted-Count':str(sum(r['deleted'] for r in results))},response,len(response)

def operate(server, method, target, headers, body, gateway, operation_id=None):
    ids = json.loads((gateway.STATE / 'fixture-allowlist.json').read_text())
    v2_file=gateway.STATE/'v2-asset-allowlist.json'
    v2=json.loads(v2_file.read_text()) if v2_file.exists() else {}
    if not isinstance(v2,dict) or len(v2)>1000:raise ValueError('Invalid V2 asset allowlist')
    reader_file=gateway.STATE/'reader-asset-allowlist.json'
    reader_ids=json.loads(reader_file.read_text()).get('bookIds',[]) if reader_file.exists() else []
    if (not isinstance(reader_ids,list) or len(reader_ids)>8 or
            len(set(reader_ids))!=len(reader_ids) or any(not isinstance(book,str) or
            not re.fullmatch(r'[0-9a-f-]{32,36}',book) for book in reader_ids)):
        raise ValueError('Invalid reader asset allowlist')
    native_file=gateway.STATE/'reader-native-asset-allowlist.json'
    native_assets=json.loads(native_file.read_text()) if native_file.exists() else {}
    if not isinstance(native_assets,dict) or len(native_assets)>64 or any(
            book not in reader_ids or not isinstance(key,str) or
            not re.fullmatch(r'powersync-lab/reader/'+re.escape(book)+
                             r'/(?:point|shape|resource/pb|resource/data)/[A-Za-z0-9#._-]+',key)
            for key,book in native_assets.items()):
        raise ValueError('Invalid exact native reader asset allowlist')
    dynamic_file=gateway.STATE/'reader-native-dynamic-book-allowlist.json'
    dynamic_books=json.loads(dynamic_file.read_text()) if dynamic_file.exists() else []
    if (not isinstance(dynamic_books,list) or len(dynamic_books)>8 or
            any(not isinstance(book,str) or book not in reader_ids for book in dynamic_books) or
            len(set(dynamic_books))!=len(dynamic_books)):
        raise ValueError('Invalid dynamic native reader book allowlist')
    url = urlsplit(target)
    if method=='POST':return bulk_delete(server,target,headers,body,gateway,ids,v2,operation_id)
    if url.path.rstrip('/') == '/' + BUCKET and method == 'GET':
        query = parse_qs(url.query, keep_blank_values=True)
        if set(query) - {'prefix', 'max-keys', 'marker', 'delimiter'} or any(len(v) != 1 for v in query.values()):
            raise NotImplementedError('Unsupported listing options')
        prefix = query.get('prefix', [''])[0]
        reader_listing=any(prefix.rstrip('/') in (f'{UID}/reader/{book}',
                           f'{UID}/reader/{book}/book',f'{UID}/reader/{book}/thumbnail') for book in reader_ids)
        if not reader_listing:scope('/' + BUCKET + '/' + prefix.rstrip('/') + '/scope-check', ids)
        # Prefix must include a complete notebook directory, never an account-wide prefix.
        if not reader_listing and not any(prefix == UID + '/note/' + fixture or prefix.startswith(UID + '/note/' + fixture + '/') for fixture in ids):
            raise ValueError('Listing outside disposable notebook')
        if query.get('delimiter', [''])[0]:
            raise NotImplementedError('Delimited listing unsupported')
        maximum = int(query.get('max-keys', ['1000'])[0])
        if not 1 <= maximum <= 1000:
            raise ValueError('Invalid listing bound')
        with server.lock:
            device = authenticate(method, '/' + BUCKET + '/', headers, server.registry)
            if getattr(server,'is_revoked',lambda d:False)(device):raise ValueError('Revoked lab device')
        docs = gateway.request('/neocloud/_all_docs?include_docs=true', auth=server.auth)
        marker = query.get('marker', [''])[0]
        objects = sorted((row['doc'] for row in docs['rows'] if row.get('doc', {}).get('type') == 'labAsset'
                          and row['doc'].get('ossKey', '').startswith(prefix)
                          and (not reader_listing or any(reader_key_admitted(row['doc']['ossKey'], book, native_assets, dynamic_books) for book in reader_ids))
                          and row['doc']['ossKey'] > marker), key=lambda d: d['ossKey'])
        truncated = len(objects) > maximum
        objects = objects[:maximum]
        entries = ''.join('<Contents><Key>' + escape(d['ossKey']) + '</Key><LastModified>'
                          + datetime.fromtimestamp(d['updatedAt']/1000, timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.000Z')
                          + '</LastModified><ETag>"' + d['md5'].upper() + '"</ETag><Size>' + str(d['bytes'])
                          + '</Size><StorageClass>Standard</StorageClass></Contents>' for d in objects)
        xml = ('<?xml version="1.0" encoding="UTF-8"?><ListBucketResult><Name>' + BUCKET
               + '</Name><Prefix>' + escape(prefix) + '</Prefix><Marker>' + escape(marker)
               + '</Marker><MaxKeys>' + str(maximum) + '</MaxKeys><IsTruncated>' + str(truncated).lower()
               + '</IsTruncated><NextMarker>' + escape(objects[-1]['ossKey'] if truncated else '')
               + '</NextMarker>' + entries + '</ListBucketResult>').encode()
        return device, 200, {'Content-Type': 'application/xml'}, xml, len(xml)
    path, key, fixture = scope(target, ids, v2,reader_ids,native_assets,dynamic_books)
    with server.lock:
        device = authenticate(method, path, headers, server.registry)
        if getattr(server,'is_revoked',lambda d:False)(device):raise ValueError('Revoked lab device')
    if method not in ('PUT', 'GET', 'HEAD', 'DELETE'):
        raise NotImplementedError('Unsupported object method')
    digest = base64.b64encode(hashlib.md5(body).digest()).decode()
    if method == 'PUT' and headers.get('Content-MD5') and not hmac.compare_digest(headers['Content-MD5'], digest):
        raise ValueError('Content MD5 mismatch')
    if reader_upload_fault(gateway.STATE, method, key, fixture, device):
        response = b'<Error><Code>ServiceUnavailable</Code></Error>'
        return device, 503, {'Content-Type': 'application/xml',
                            'X-PowerSync-Injected-Fault': 'reader-upload-503'}, response, len(response)
    doc_id = 'labasset-' + hashlib.sha256(key.encode()).hexdigest()
    endpoint = '/neocloud/' + doc_id
    try:
        old = gateway.request(endpoint, auth=server.auth)
    except HTTPError as error:
        with error:
            if error.code != 404:
                raise
        old = None
    response_headers = {'x-oss-request-id': 'powersync-disposable-lab', 'Content-Type': 'application/octet-stream'}
    if method == 'PUT':
        doc = {'user': UID, 'dbId': UID + '-ASSET-' + fixture, 'documentUniqueId': fixture,
               'type': 'labAsset', 'ossKey': key, 'sha256': hashlib.sha256(body).hexdigest(),
               'md5': hashlib.md5(body).hexdigest(), 'bytes': len(body),
               'updatedAt': int(time.time() * 1000),
               'sourceDeviceId': device['deviceId'], 'sourceOperationId': operation_id,
               '_attachments': {'content': {'content_type': headers.get('Content-Type', 'application/octet-stream'),
                                             'data': base64.b64encode(body).decode()}}}
        if old:
            doc['_rev'] = old['_rev']
        saved = gateway.request(endpoint, 'PUT', doc, server.auth)
        response_headers.update({'X-PowerSync-Backend-Id': doc_id,
                                 'X-PowerSync-Backend-Rev': saved['rev'],
                                 'X-PowerSync-Object-SHA256': doc['sha256']})
        response_headers['ETag'] = '"' + doc['md5'].upper() + '"'
        return device, 200, response_headers, b'', 0
    if not old:
        response = b'<Error><Code>NoSuchKey</Code></Error>'
        return device, 404, {'Content-Type': 'application/xml'}, response, len(response)
    response_headers.update(ETag='"' + old['md5'].upper() + '"',
                            **{'Last-Modified': formatdate(old['updatedAt'] / 1000, usegmt=True),
                               'Content-MD5': base64.b64encode(bytes.fromhex(old['md5'])).decode()})
    response_headers.update({'X-PowerSync-Backend-Id': doc_id,
                             'X-PowerSync-Backend-Rev': old['_rev'],
                             'X-PowerSync-Object-SHA256': old['sha256']})
    fault = injected_fault(gateway.STATE, method, key, fixture, device)
    if fault is not None:
        response = ('<Error><Code>' + ('NoSuchKey' if fault == 404 else 'ServiceUnavailable')
                    + '</Code></Error>').encode()
        response_headers.update({'Content-Type': 'application/xml',
                                 'X-PowerSync-Injected-Fault': str(fault)})
        return device, fault, response_headers, response, len(response)
    if method == 'DELETE':
        saved = gateway.request(endpoint, 'PUT', {'_rev': old['_rev'], '_deleted': True}, server.auth)
        response_headers['X-PowerSync-Backend-Rev'] = saved['rev']
        return device, 204, response_headers, b'', 0
    if method == 'HEAD':
        return device, 200, response_headers, b'', old['bytes']
    doc = gateway.request(endpoint + '?attachments=true', auth=server.auth)
    content = base64.b64decode(doc['_attachments']['content']['data'], validate=True)
    if len(content) != old['bytes'] or hashlib.sha256(content).hexdigest() != old['sha256']:
        raise ValueError('Stored lab attachment integrity failure')
    mode=transfer_fault(gateway.STATE,method,key,fixture,device)
    if mode:
        response_headers['X-PowerSync-Transfer-Fault']=mode
        if mode=='corrupt' and content:content=bytes([content[0]^1])+content[1:]
        if mode=='truncate':return device,200,response_headers,content[:len(content)//2],len(content)
    return device, 200, response_headers, content, len(content)
