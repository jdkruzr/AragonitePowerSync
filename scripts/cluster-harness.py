#!/usr/bin/env python3
"""Loopback-only stock-ksync routing probe, not a Couchbase implementation.

Never retains request bodies, query values, or authorization values. Unknown
routes fail closed; no note replication or object upload is accepted.
"""
import ipaddress
import argparse
import hashlib
import json
import re
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit, parse_qs

LAB_ID = 'powersync_lab'

def lab_host(host):
    address = ipaddress.IPv4Address(host)
    if address.is_unspecified or address.is_multicast or not (address.is_loopback or address.is_private):
        raise ValueError('Lab listener requires a concrete private or loopback IPv4 address')
    return str(address)

def cluster_definition(port, blip_port=None, advertised_host='127.0.0.1'):
    advertised_host = lab_host(advertised_host)
    http = f'http://{advertised_host}:{port}'
    ws = f'ws://{advertised_host}:{blip_port or port}/neocloud'
    host = {key: http for key in (
        'onyxHostBaseUrl', 'onyxCloudDataHostBaseUrl', 'onyxLogHostBaseUrl',
        'onyxContentHostBaseUrl', 'onyxSend2BooxHostBaseUrl', 'shopBookUrl',
        'onyxMaterialHostBaseUrl')}
    host.update(onyxCouchbaseWSSHost=ws, onyxMessageReplicator=ws)
    oss = {key: http for key in (
        'ossNoteEndPoint', 'ossDeprecatedEndPoint', 'ossAIEndPoint',
        'ossLogEndpoint', 'ossTestEndpoint')}
    oss.update({key: 'powersync-lab' for key in (
        'ossNoteBucket', 'ossDeprecatedBucket', 'ossAIBucket',
        'ossLogBucket', 'ossTestBucket')})
    # Vendor setLang also sets serverId. Keep both equal; otherwise parsing can
    # replace a production cluster under its ID instead of adding the lab ID.
    return {'serverId': LAB_ID, 'lang': LAB_ID, 'name': 'PowerSync local lab',
            'region': 'POWERSYNC_LAB', 'host': host, 'ossConfig': oss}

def safe_event(method, target, headers, body):
    url = urlsplit(target)
    path = re.sub(r'[0-9a-fA-F]{24,}|[^/]*@[^/]*', '<redacted>', url.path)
    return {'at': time.time(), 'method': method, 'path': path,
            'queryKeys': sorted(parse_qs(url.query, keep_blank_values=True)),
            'bodyBytes': len(body), 'bodySha256': hashlib.sha256(body).hexdigest(),
            'authorizationPresent': 'Authorization' in headers,
            'upgrade': headers.get('Upgrade', ''),
            'websocketProtocol': headers.get('Sec-WebSocket-Protocol', '')}

class Harness(ThreadingHTTPServer):
    def __init__(self, port, output, allow_anonymous=False, blip_port=None):
        super().__init__(('127.0.0.1', port), Handler)
        self.output = output
        self.lock = threading.Lock()
        self.allow_anonymous = allow_anonymous
        self.blip_port = blip_port

class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'
    def log_message(self, *args):
        pass
    def respond(self, status, value):
        body = json.dumps(value).encode()
        self.send_response(status)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(body)
        self.close_connection = True
    def handle_probe(self):
        try:
            size = int(self.headers.get('Content-Length', '0'))
        except ValueError:
            self.respond(400, {'error': 'invalid length'}); return
        if 'Transfer-Encoding' in self.headers or not 0 <= size <= 1048576:
            self.respond(413, {'error': 'unsupported body size or framing'}); return
        self.connection.settimeout(10)
        body = self.rfile.read(size)
        if len(body) != size:
            self.respond(400, {'error': 'incomplete body'}); return
        event = safe_event(self.command, self.path, self.headers, body)
        path = urlsplit(self.path).path
        status, response = 401, {'result': -1, 'error': 'PowerSync routing probe only'}
        if path == '/health' and self.command == 'GET':
            status, response = 200, {'ok': True, 'replicationImplemented': False}
        elif path.endswith('/serverInfos/ssl') and self.command == 'GET':
            status, response = 200, {'result': 0, 'data': {'results': [cluster_definition(self.server.server_port, self.server.blip_port)]}}
        elif path.endswith('/devices/logout') and self.command == 'POST':
            status, response = 200, {'result': 0}
        elif path.endswith('/users/checkSyncUser') and self.command == 'POST' and self.server.allow_anonymous:
            status, response = 200, {'data': True}
        elif self.headers.get('Upgrade', '').lower() == 'websocket':
            status, response = 501, {'error': 'BLIP replication not implemented'}
        event['responseStatus'] = status
        with self.server.lock:
            with self.server.output.open('a') as stream:
                stream.write(json.dumps(event) + '\n')
        self.respond(status, response)
    do_GET = do_POST = do_PUT = do_DELETE = do_PATCH = do_OPTIONS = handle_probe

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=18765)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--marker', type=Path, required=True)
    parser.add_argument('--allow-anonymous-probe', action='store_true',
                        help='allow anonymous gateway setup; WebSocket replication remains rejected')
    parser.add_argument('--blip-port', type=int, help='separate loopback BLIP probe port')
    args = parser.parse_args()
    if not 1024 <= args.port <= 65535:
        parser.error('port must be 1024..65535')
    if args.blip_port is not None and not 1024 <= args.blip_port <= 65535:
        parser.error('BLIP port must be 1024..65535')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.marker.parent.mkdir(parents=True, exist_ok=True)
    server = Harness(args.port, args.output, args.allow_anonymous_probe, args.blip_port)
    args.marker.write_text(json.dumps([cluster_definition(args.port, args.blip_port)], indent=2) + '\n')
    print(f'PowerSync routing probe listening on 127.0.0.1:{args.port}', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

if __name__ == '__main__':
    main()
